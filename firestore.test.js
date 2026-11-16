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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

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

test("Unauthenticated user cannot read or write user profile or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
});

test("User can create and read own profile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredLanguage: "en",
      preferredModel: "gemini-3.8-flash",
      totalRequests: 0,
      totalImagesGenerated: 0,
      totalDocumentsAnalyzed: 0,
      totalEmailsAssisted: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update with ghost field on user profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredLanguage: "en",
      preferredModel: "gemini-3.8-flash",
      totalRequests: 0,
      totalImagesGenerated: 0,
      totalDocumentsAnalyzed: 0,
      totalEmailsAssisted: 0,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user access to conversations and documents is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set({
        userId: ALICE_UID,
        title: "My Chat",
        modelId: "gemini-3.8-flash",
        lastMessagePreview: "Hello",
        messageCount: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .get()
  );
});

test("Chat message requires existing parent conversation and valid role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  // Fails when parent conversation does not exist
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("missing_conv")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        conversationId: "missing_conv",
        role: "user",
        content: "Hello",
        modelId: "gemini-3.8-flash",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Create parent conversation first
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set({
        userId: ALICE_UID,
        title: "Test Conv",
        modelId: "gemini-3.8-flash",
        lastMessagePreview: "Hello",
        messageCount: 1,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        conversationId: "conv_1",
        role: "user",
        content: "Hello AI",
        modelId: "gemini-3.8-flash",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Email draft terminal state locking blocks updates after dispatched", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("emailDrafts")
    .doc("draft_1");

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      recipient: "team@example.com",
      subject: "Status",
      body: "All good",
      priority: "medium",
      summary: "Status update",
      extractedTasks: ["Send update"],
      status: "draft",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    ref.update({
      status: "dispatched",
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    ref.update({
      subject: "Tampered",
      updatedAt: nowTs(),
    })
  );
});
