const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 1000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("sellers").where("demoListed", "==", true).get());
  await assertFails(unauthDb.collection("products").where("demoListed", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("buyerUid", "==", ALICE_UID).get());
});

test("User profile, virtual cards, transactions, and tickets enforce strict owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  const profilePayload = {
    userId: ALICE_UID,
    displayName: "Alice Vault",
    customerId: "VP-102938",
    coins: 500,
    unlimitedMode: false,
    unlimitedBonusBalance: 0,
    totalSpent: 0,
    pinEnabled: false,
    simPin: "1234",
    soundEnabled: true,
    lastDailyClaimDay: "",
    sellerAccountId: "",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(profilePayload));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ coins: 9999, updatedAt: pastDate() }));

  // Card create & deactivate toggle
  const cardPayload = {
    userId: ALICE_UID,
    tierKey: "STARTER",
    cardTitle: "Starter Classic",
    cardNetworkLabel: "VAULT CLASSIC DEMO",
    cardholderName: "ALICE VAULT",
    cardNumber: "9901 4820 1192 4012",
    expiryDisplay: "12/99",
    cvvDisplay: "904",
    balance: 1000,
    unlimitedBalance: false,
    cardActive: true,
    accentColorHex: "#38BDF8",
    upgradeLevel: 1,
    animatedStyle: false,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cards").doc("card_1").set(cardPayload));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cards").doc("card_1").update({
    cardActive: false,
    updatedAt: pastDate(),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cards").get());

  // Shadow update / ghost field rejection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cards").doc("card_ghost").set({
      ...cardPayload,
      id: "card_ghost",
    })
  );

  // Transaction create + idempotency
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("txn_1").set({
      userId: ALICE_UID,
      transactionRef: "TXN-1001",
      idempotencyKey: "txn_1",
      type: "PAYMENT",
      amount: 120.5,
      currencyLabel: "SIM USD",
      merchantName: "Apex Tech Gear",
      sellerId: "demo_store_apex",
      sellerName: "Apex Tech Gear",
      cardId: "card_1",
      cardSummary: "Starter •••• 4012",
      orderId: "ord_1",
      status: "SIMULATED_SUCCESS",
      note: "Simulated checkout",
      createdAt: ts,
    })
  );

  // Support ticket create & status update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      ticketCode: "SUP-2041",
      subject: "Card Activation Check",
      description: "Testing simulated card activation flow.",
      category: "Card Activation",
      status: "OPEN",
      resolutionNote: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tickets").doc("tkt_1").update({
      status: "RESOLVED",
      resolutionNote: "Simulator card reactivated.",
      updatedAt: pastDate(),
    })
  );
});

test("Sellers, products, orders, and reviews enforce query filters, role-based updates, and terminal locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  // Bob creates his Seller Store
  await assertSucceeds(
    bobDb.collection("sellers").doc("seller_bob").set({
      sellerOwnerUid: BOB_UID,
      sellerCode: "SEL-BOB01",
      storeName: "Bob Digital Hub",
      description: "Simulated electronics storefront",
      category: "Electronics",
      logoToneHex: "#F59E0B",
      demoVerified: true,
      demoListed: true,
      qrPayload: "VAULTPLAY_QR|seller_bob|Bob Digital Hub",
      ratingSum: 0,
      reviewCount: 0,
      completedOrdersCount: 0,
      virtualEarnings: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot hijack Bob's store name, nor query unfiltered sellers
  await assertFails(aliceDb.collection("sellers").get());
  await assertSucceeds(aliceDb.collection("sellers").where("demoListed", "==", true).get());
  await assertFails(
    aliceDb.collection("sellers").doc("seller_bob").update({
      storeName: "Hijacked",
      updatedAt: pastDate(),
    })
  );

  // Bob adds a product
  await assertSucceeds(
    bobDb.collection("products").doc("prod_1").set({
      sellerId: "seller_bob",
      sellerOwnerUid: BOB_UID,
      storeName: "Bob Digital Hub",
      name: "Holo Watch Demo",
      description: "Virtual collector watch",
      category: "Wearables",
      price: 250,
      stockCount: 10,
      iconKey: "watch",
      demoListed: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice places an order with Bob's store
  const orderPayload = {
    orderCode: "ORD-7712",
    buyerUid: ALICE_UID,
    buyerName: "Alice Vault",
    sellerId: "seller_bob",
    sellerOwnerUid: BOB_UID,
    sellerName: "Bob Digital Hub",
    itemsSummary: "1x Holo Watch Demo",
    itemCount: 1,
    totalAmount: 250,
    cardId: "card_1",
    cardSummary: "Starter •••• 4012",
    transactionRef: "TXN-1002",
    status: "PAYMENT_SIMULATED",
    trackingEvents: [{ stage: "ORDER_PLACED", label: "Order Placed", timestampMillis: Date.now() }],
    refundStatus: "NONE",
    refundReason: "",
    sellerResponse: "",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("orders").doc("ord_1").set(orderPayload));

  // Buyer and seller filtered queries succeed; unfiltered fails
  await assertFails(aliceDb.collection("orders").get());
  await assertSucceeds(aliceDb.collection("orders").where("buyerUid", "==", ALICE_UID).get());
  await assertSucceeds(bobDb.collection("orders").where("sellerOwnerUid", "==", BOB_UID).get());

  // Buyer can credit seller earnings by exact increment
  await assertSucceeds(
    aliceDb.collection("sellers").doc("seller_bob").update({
      virtualEarnings: 250,
      completedOrdersCount: 1,
      updatedAt: pastDate(),
    })
  );

  // Seller confirms order; buyer cannot confirm their own order with a real seller
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "SELLER_CONFIRMED",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("orders").doc("ord_1").update({
      status: "SELLER_CONFIRMED",
      updatedAt: pastDate(),
    })
  );

  // Buyer requests refund
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").update({
      refundStatus: "REQUESTED",
      refundReason: "Testing simulated refund flow",
      updatedAt: pastDate(),
    })
  );

  // Seller approves refund -> terminal state REFUNDED
  await assertSucceeds(
    bobDb.collection("orders").doc("ord_1").update({
      status: "REFUNDED",
      refundStatus: "APPROVED",
      sellerResponse: "Approved virtual refund.",
      updatedAt: pastDate(),
    })
  );

  // Terminal state lock blocks further mutations
  await assertFails(
    bobDb.collection("orders").doc("ord_1").update({
      status: "DELIVERED",
      updatedAt: pastDate(),
    })
  );

  // Alice leaves a review
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      sellerId: "seller_bob",
      orderId: "ord_1",
      buyerUid: ALICE_UID,
      buyerName: "Alice Vault",
      rating: 5,
      comment: "Great simulator checkout!",
      demoListed: true,
      createdAt: ts,
    })
  );
});
