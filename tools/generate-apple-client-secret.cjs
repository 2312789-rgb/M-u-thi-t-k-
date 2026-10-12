const { readFileSync } = require("node:fs");
const { sign } = require("node:crypto");

const [teamId, keyId, servicesId, keyPath] = process.argv.slice(2);
if (!teamId || !keyId || !servicesId || !keyPath) {
    console.error("Usage: node tools/generate-apple-client-secret.cjs <TEAM_ID> <KEY_ID> <SERVICES_ID> <PATH_TO_P8>");
    process.exit(1);
}

const encode = value => Buffer.from(JSON.stringify(value)).toString("base64url");
const issuedAt = Math.floor(Date.now() / 1000);
const header = encode({ alg: "ES256", kid: keyId, typ: "JWT" });
const claims = encode({
    iss: teamId,
    iat: issuedAt,
    exp: issuedAt + 60 * 60 * 24 * 180,
    aud: "https://appleid.apple.com",
    sub: servicesId
});
const signingInput = `${header}.${claims}`;
const signature = sign("sha256", Buffer.from(signingInput), {
    key: readFileSync(keyPath),
    dsaEncoding: "ieee-p1363"
}).toString("base64url");

console.log(`${signingInput}.${signature}`);
