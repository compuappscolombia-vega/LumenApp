const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_director_1";
const BOB_UID = "bob_director_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any LumenReel collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      ownerId: ALICE_UID,
      title: "Unauthorized Film",
      genreStyle: "Neo-Noir",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner creates profile, project, draft sequence, agent message, and learning lesson", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Elena Morales",
      email: "elena@lumenreel.studio",
      role: "director",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject ghost `id` field when @DocumentId is used in Kotlin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_bad").set({
      id: "proj_bad",
      ownerId: ALICE_UID,
      title: "Bad Payload",
      genreStyle: "IMAX",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      ownerId: ALICE_UID,
      title: "El Puerto de Bruma",
      genreStyle: "Thriller Psicológico Neo-Noir",
      defaultSourceType: "book",
      continuityBible: "Regla 1: lluvia constante.",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // List empty sequences & populated projects
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        title: "Capítulo I: El Muelle",
        versionStatus: "draft",
        sourceType: "book",
        directorStyle: "Thriller Psicológico Neo-Noir",
        aspectRatio: "2.39:1",
        logline: "Un inspector busca un barco desaparecido entre la niebla.",
        directorVision: "Tonos de sodio y sombras densas.",
        continuitySummary: "El faro parpadea cada 4 segundos.",
        sequenceData: {
          cameraSystem: "RED V-Raptor XL",
          lensPackage: "Cooke Anamorphic",
        },
        createdAt: past,
        updatedAt: past,
      })
  );

  // Finalize sequence and rate finalized sequence
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        versionStatus: "final",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        sequenceRating: 5,
        updatedAt: new Date(Date.now() - 250),
      })
  );

  // Finalized sequence content cannot be overwritten or deleted
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        title: "Edited Final",
        updatedAt: new Date(Date.now() - 100),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .delete()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("agent_messages")
      .doc("msg_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        role: "agent",
        focus: "council",
        content: "Propuesta de iluminación Kelvin a 3200K.",
        actions: [{ toolName: "actualizar_biblia_continuidad", summary: "Actualizada", affectedArea: "biblia" }],
        createdAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("learning_memory")
      .doc("lesson_1")
      .set({
        ownerId: ALICE_UID,
        category: "camera",
        insight: "Cooke Anamorphic a T2.0 refuerza la intimidad en lluvia.",
        rating: 5,
        sourceProjectTitle: "El Puerto de Bruma",
        createdAt: past,
      })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice workspace", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("projects").doc("proj_a").set({
      ownerId: ALICE_UID,
      title: "Proyecto Privado",
      genreStyle: "IMAX",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_a").update({
      continuityBible: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Rejects future timestamps and privilege escalation on user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Admin Spoofer",
      email: "a@b.com",
      role: "admin",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Clock Tamperer",
      email: "a@b.com",
      role: "director",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
