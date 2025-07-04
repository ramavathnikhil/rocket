console.log('composeApp.js: script loaded');

// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyBp2Xzont2ixx7hmYcJ1L0_BJQ8bjAUtQc",
    authDomain: "rapido-captain-test.firebaseapp.com",
    databaseURL: "https://rapido-captain-test.firebaseio.com",
    projectId: "rapido-captain-test",
    storageBucket: "rapido-captain-test.firebasestorage.app",
    messagingSenderId: "301187883692",
    appId: "1:301187883692:web:dc6fa5c806c9e4e821f509"
};

// Make Firebase config available globally
window.firebaseConfig = firebaseConfig;

// Global callback function that Kotlin can set
window.githubValidationCallback = null;

// Self-contained GitHub token validation
function handleGitHubTokenValidation(token) {
    console.log('handleGitHubTokenValidation: === STARTING VALIDATION ===');
    console.log('handleGitHubTokenValidation: token provided =', !!token);
    
    // Check if Firebase is available
    if (typeof window.firebase === 'undefined') {
        console.error('handleGitHubTokenValidation: Firebase not available');
        if (window.githubValidationCallback) {
            window.githubValidationCallback(false, 'Firebase not available');
        }
        return;
    }
    
    // Check if user is authenticated
    const auth = window.firebase.auth();
    const currentUser = auth.currentUser;
    
    if (!currentUser) {
        console.error('handleGitHubTokenValidation: User not authenticated');
        if (window.githubValidationCallback) {
            window.githubValidationCallback(false, 'User must be logged in to validate GitHub tokens');
        }
        return;
    }
    
    console.log('handleGitHubTokenValidation: User authenticated, calling Firebase Function...');
    
    // Call Firebase Function
    try {
        const functions = window.firebase.functions();
        const validateTokenFunction = functions.httpsCallable('validateGitHubToken');
        
        validateTokenFunction({ token: token })
            .then(result => {
                console.log('handleGitHubTokenValidation: Function result =', result);
                const data = result.data;
                const isValid = data && data.valid === true;
                const userInfo = data && data.user;
                
                console.log('handleGitHubTokenValidation: Token valid =', isValid);
                
                if (isValid && userInfo) {
                    console.log('handleGitHubTokenValidation: User =', userInfo.login);
                    if (window.githubValidationCallback) {
                        window.githubValidationCallback(true, `Token valid for user: ${userInfo.login}`);
                    }
                } else {
                    if (window.githubValidationCallback) {
                        window.githubValidationCallback(false, 'Invalid GitHub token');
                    }
                }
            })
            .catch(error => {
                console.error('handleGitHubTokenValidation: Function error =', error);
                const errorMessage = error.message || 'Unknown error validating token';
                if (window.githubValidationCallback) {
                    window.githubValidationCallback(false, errorMessage);
                }
            });
    } catch (error) {
        console.error('handleGitHubTokenValidation: Exception =', error);
        if (window.githubValidationCallback) {
            window.githubValidationCallback(false, error.message || 'Exception during validation');
        }
    }
}

// Function to set up validation from Kotlin
function setupGitHubValidation(token) {
    console.log('setupGitHubValidation: Setting up validation for token');
    // Use setTimeout to run async without blocking
    setTimeout(() => {
        handleGitHubTokenValidation(token);
    }, 0);
    return true; // Return immediately
}

// Simple GitHub token validation that handles everything in JavaScript
function validateGitHubTokenSimple(token) {
    console.log('validateGitHubTokenSimple: === FUNCTION ENTRY ===');
    console.log('validateGitHubTokenSimple: token provided =', !!token);
    
    // Check if Firebase is available
    if (typeof window.firebase === 'undefined') {
        console.error('validateGitHubTokenSimple: Firebase not available');
        return Promise.resolve(false);
    }
    
    // Check if user is authenticated
    const auth = window.firebase.auth();
    const currentUser = auth.currentUser;
    
    if (!currentUser) {
        console.error('validateGitHubTokenSimple: User not authenticated');
        return Promise.resolve(false);
    }
    
    console.log('validateGitHubTokenSimple: User authenticated, calling Firebase Function...');
    
    // Call Firebase Function
    try {
        const functions = window.firebase.functions();
        const validateTokenFunction = functions.httpsCallable('validateGitHubToken');
        
        return validateTokenFunction({ token: token })
            .then(result => {
                console.log('validateGitHubTokenSimple: Function result =', result);
                const data = result.data;
                const isValid = data && data.valid === true;
                console.log('validateGitHubTokenSimple: Token valid =', isValid);
                return isValid;
            })
            .catch(error => {
                console.error('validateGitHubTokenSimple: Function error =', error);
                return false;
            });
    } catch (error) {
        console.error('validateGitHubTokenSimple: Exception =', error);
        return Promise.resolve(false);
    }
}

// Even simpler version that just returns a boolean directly (no Promise)
function validateGitHubTokenSync(token) {
    console.log('validateGitHubTokenSync: === FUNCTION ENTRY ===');
    console.log('validateGitHubTokenSync: This is a synchronous test function');
    
    // Just return true for now to test if sync functions work
    return true;
}

// Test Firebase Functions availability
function testFirebaseFunctions() {
    console.log('testFirebaseFunctions: === FUNCTION ENTRY ===');
    console.log('testFirebaseFunctions: Checking Firebase availability...');
    console.log('testFirebaseFunctions: window.firebase =', typeof window.firebase);
    
    if (typeof window.firebase === 'undefined') {
        console.error('testFirebaseFunctions: Firebase not loaded!');
        return Promise.resolve(false);
    }
    
    console.log('testFirebaseFunctions: firebase.functions =', typeof window.firebase.functions);
    
    if (typeof window.firebase.functions === 'undefined') {
        console.error('testFirebaseFunctions: Firebase Functions not loaded!');
        return Promise.resolve(false);
    }
    
    try {
        const functions = window.firebase.functions();
        console.log('testFirebaseFunctions: functions instance =', functions);
        console.log('testFirebaseFunctions: functions.httpsCallable =', typeof functions.httpsCallable);
        
        // Try to create a callable function reference
        const testFunction = functions.httpsCallable('validateGitHubToken');
        console.log('testFirebaseFunctions: testFunction =', testFunction);
        
        console.log('testFirebaseFunctions: === FUNCTION EXIT SUCCESS ===');
        return Promise.resolve(true);
    } catch (error) {
        console.error('testFirebaseFunctions: Error creating functions instance:', error);
        console.log('testFirebaseFunctions: === FUNCTION EXIT ERROR ===');
        return Promise.resolve(false);
    }
}

// Check Firebase Auth status
function checkFirebaseAuth() {
    console.log('checkFirebaseAuth: === FUNCTION ENTRY ===');
    
    if (typeof window.firebase === 'undefined' || !window.firebase.auth) {
        console.error('checkFirebaseAuth: Firebase Auth not available');
        return Promise.resolve({
            authenticated: false,
            error: 'Firebase Auth not available'
        });
    }
    
    const auth = window.firebase.auth();
    const currentUser = auth.currentUser;
    
    console.log('checkFirebaseAuth: currentUser =', currentUser);
    console.log('checkFirebaseAuth: currentUser type =', typeof currentUser);
    
    if (currentUser) {
        console.log('checkFirebaseAuth: User is authenticated');
        console.log('checkFirebaseAuth: User UID =', currentUser.uid);
        console.log('checkFirebaseAuth: User email =', currentUser.email);
        
        // Get ID token for debugging
        return currentUser.getIdToken().then(token => {
            console.log('checkFirebaseAuth: ID token obtained (length):', token ? token.length : 'null');
            return {
                authenticated: true,
                uid: currentUser.uid,
                email: currentUser.email,
                hasToken: !!token
            };
        }).catch(error => {
            console.error('checkFirebaseAuth: Error getting ID token:', error);
            return {
                authenticated: true,
                uid: currentUser.uid,
                email: currentUser.email,
                hasToken: false,
                tokenError: error.message
            };
        });
    } else {
        console.log('checkFirebaseAuth: User is NOT authenticated');
        return Promise.resolve({
            authenticated: false,
            error: 'User not logged in'
        });
    }
}

// Explicitly register on window object
window.testFirebaseFunctions = testFirebaseFunctions;
window.checkFirebaseAuth = checkFirebaseAuth;
window.validateGitHubTokenSimple = validateGitHubTokenSimple;
window.validateGitHubTokenSync = validateGitHubTokenSync;
window.setupGitHubValidation = setupGitHubValidation;

console.log('composeApp.js: Firebase configuration loaded');
console.log('composeApp.js: GitHub API functions now handled by Firebase Functions');

// Verify function registration
console.log('composeApp.js: Verifying testFirebaseFunctions registration...');
console.log('composeApp.js: window.testFirebaseFunctions =', typeof window.testFirebaseFunctions);
console.log('composeApp.js: window.testFirebaseFunctions available =', !!window.testFirebaseFunctions);

// Test the function immediately to see if it works
console.log('composeApp.js: Testing function immediately...');
try {
    const result = window.testFirebaseFunctions();
    console.log('composeApp.js: Immediate test result =', result);
    if (result && typeof result.then === 'function') {
        result.then(success => {
            console.log('composeApp.js: Immediate test resolved with =', success);
        }).catch(error => {
            console.error('composeApp.js: Immediate test rejected with =', error);
        });
    }
} catch (error) {
    console.error('composeApp.js: Error in immediate test =', error);
} 