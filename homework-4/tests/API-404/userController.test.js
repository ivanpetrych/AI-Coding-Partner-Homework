/**
 * Unit Tests: userController.js
 * Tests for the getUserById function fix (API-404)
 * Tests that req.params.id is correctly parsed as an integer before comparison
 */

const assert = require('assert');
const { getUserById, getAllUsers } = require('../../demo-bug-fix/src/controllers/userController');

// Helper function to create mock req and res objects
function createMockReqRes(paramId) {
  const req = {
    params: {
      id: paramId  // This will be a string, as Express always provides
    }
  };
  
  const res = {
    status: function(code) {
      this.statusCode = code;
      return this;
    },
    json: function(data) {
      this.jsonData = data;
      return this;
    }
  };
  
  return { req, res };
}

/**
 * Test Suite 1: Valid user IDs (should return 200 + user object)
 */
async function testValidUserId123() {
  const { req, res } = createMockReqRes('123');  // String, as Express provides
  await getUserById(req, res);
  
  assert.strictEqual(res.statusCode, 200, 'should return status 200 for valid user ID 123');
  assert.strictEqual(res.jsonData.id, 123, 'should return user with numeric id 123');
  assert.strictEqual(res.jsonData.name, 'Alice Smith', 'should return correct user name');
  console.log('✅ Test 1 PASS: Valid user ID 123 returns 200 + user object');
}

async function testValidUserId456() {
  const { req, res } = createMockReqRes('456');  // String, as Express provides
  await getUserById(req, res);
  
  assert.strictEqual(res.statusCode, 200, 'should return status 200 for valid user ID 456');
  assert.strictEqual(res.jsonData.id, 456, 'should return user with numeric id 456');
  assert.strictEqual(res.jsonData.name, 'Bob Johnson', 'should return correct user name');
  console.log('✅ Test 2 PASS: Valid user ID 456 returns 200 + user object');
}

/**
 * Test Suite 2: Non-existent user IDs (should return 404 + error message)
 */
async function testNonExistentUserId() {
  const { req, res } = createMockReqRes('999');  // Valid numeric format, but user doesn't exist
  await getUserById(req, res);
  
  assert.strictEqual(res.statusCode, 404, 'should return status 404 for non-existent user ID');
  assert.strictEqual(res.jsonData.error, 'User not found', 'should return error message');
  console.log('✅ Test 3 PASS: Non-existent user ID 999 returns 404 + error message');
}

/**
 * Test Suite 3: Non-numeric IDs (should return 404 because parseInt returns NaN)
 */
async function testNonNumericId() {
  const { req, res } = createMockReqRes('abc');  // Non-numeric string
  await getUserById(req, res);
  
  // parseInt('abc', 10) === NaN, which won't match any user, so 404
  assert.strictEqual(res.statusCode, 404, 'should return status 404 for non-numeric ID');
  assert.strictEqual(res.jsonData.error, 'User not found', 'should return error message for non-numeric input');
  console.log('✅ Test 4 PASS: Non-numeric ID "abc" returns 404 + error message');
}

/**
 * Test Suite 4: Edge case — empty string ID
 */
async function testEmptyStringId() {
  const { req, res } = createMockReqRes('');  // Empty string
  await getUserById(req, res);
  
  // parseInt('', 10) === NaN, which won't match any user, so 404
  assert.strictEqual(res.statusCode, 404, 'should return status 404 for empty string ID');
  assert.strictEqual(res.jsonData.error, 'User not found', 'should return error message for empty input');
  console.log('✅ Test 5 PASS: Empty string ID returns 404 + error message');
}

/**
 * Test Suite 5: Regression check — getAllUsers should still work
 */
async function testGetAllUsers() {
  const req = {};
  const res = {
    json: function(data) {
      this.jsonData = data;
      return this;
    }
  };
  
  await getAllUsers(req, res);
  
  assert.strictEqual(Array.isArray(res.jsonData), true, 'should return an array');
  assert.strictEqual(res.jsonData.length, 3, 'should return all 3 users');
  assert.strictEqual(res.jsonData[0].id, 123, 'first user should have id 123');
  console.log('✅ Test 6 PASS: getAllUsers regression check — returns all users');
}

// Run all tests
async function runTests() {
  console.log('\n========================================');
  console.log('Unit Tests: API-404 Bug Fix');
  console.log('Testing: getUserById function (line 19)');
  console.log('========================================\n');
  
  try {
    await testValidUserId123();
    await testValidUserId456();
    await testNonExistentUserId();
    await testNonNumericId();
    await testEmptyStringId();
    await testGetAllUsers();
    
    console.log('\n========================================');
    console.log('✅ ALL TESTS PASSED');
    console.log('========================================\n');
    process.exit(0);
  } catch (error) {
    console.error('\n❌ TEST FAILED:', error.message);
    console.error(error.stack);
    console.log('\n========================================');
    console.log('❌ TESTS FAILED');
    console.log('========================================\n');
    process.exit(1);
  }
}

// Execute tests
runTests();
