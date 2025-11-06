// Load environment variables from .env file
require('dotenv').config();

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Cloud Function to securely return Vision API Key
 * Only authenticated users can call this function
 * Function name: visionApiKey (to match Android app call)
 */
exports.visionApiKey = functions.https.onCall(async (data, context) => {
  // Verify the user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to retrieve API keys'
    );
  }

  try {
    // Get the API key from environment variable
    const apiKey = process.env.VISION_API_KEY;

    if (!apiKey) {
      throw new functions.https.HttpsError(
        'not-found',
        'Vision API key not configured'
      );
    }

    // Log the access for security monitoring
    console.log(`Vision API key accessed by user: ${context.auth.uid}`);

    return {
      apiKey: apiKey,
      success: true
    };
  } catch (error) {
    console.error('Error retrieving Vision API key:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to retrieve Vision API key'
    );
  }
});

/**
 * Cloud Function to securely return Gemini API Key
 * Only authenticated users can call this function
 * Function name: geminiApiKey (to match Android app call)
 */
exports.geminiApiKey = functions.https.onCall(async (data, context) => {
  // Verify the user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to retrieve API keys'
    );
  }

  try {
    // Get the API key from environment variable
    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
      throw new functions.https.HttpsError(
        'not-found',
        'Gemini API key not configured'
      );
    }

    // Log the access for security monitoring
    console.log(`Gemini API key accessed by user: ${context.auth.uid}`);

    return {
      apiKey: apiKey,
      success: true
    };
  } catch (error) {
    console.error('Error retrieving Gemini API key:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to retrieve Gemini API key'
    );
  }
});

/**
 * Optional: Function to validate API key access
 * Can be used for additional security checks
 */
exports.validateApiAccess = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  try {
    // Get user document from Firestore
    const userDoc = await admin.firestore()
      .collection('users')
      .doc(context.auth.uid)
      .get();

    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'User profile not found'
      );
    }

    const userData = userDoc.data();

    // Check if user is suspended or has restricted access
    if (userData.suspended === true) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User account is suspended'
      );
    }

    return {
      success: true,
      hasAccess: true
    };
  } catch (error) {
    console.error('Error validating API access:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to validate API access'
    );
  }
});

