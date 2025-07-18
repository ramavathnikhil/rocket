const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { Octokit } = require('@octokit/rest');
const cors = require('cors')({ 
    origin: true,
    credentials: false,
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept'],
    methods: ['GET', 'POST', 'OPTIONS']
});

// Initialize Firebase Admin
admin.initializeApp();

// Helper function to validate GitHub token and get user info
async function validateGitHubTokenInternal(token) {
    try {
        const octokit = new Octokit({
            auth: token,
        });
        
        const response = await octokit.rest.users.getAuthenticated();
        return {
            valid: true,
            user: {
                login: response.data.login,
                name: response.data.name,
                email: response.data.email,
                id: response.data.id
            }
        };
    } catch (error) {
        console.error('GitHub token validation failed:', error.message);
        return {
            valid: false,
            error: error.message
        };
    }
}

// Helper function to parse repository URL
function parseRepositoryUrl(repositoryUrl) {
    // Handle both full GitHub URLs and owner/repo format
    if (repositoryUrl.includes('github.com')) {
        const match = repositoryUrl.match(/github\.com\/([^\/]+)\/([^\/]+)/);
        if (match) {
            return { owner: match[1], repo: match[2].replace('.git', '') };
        }
    } else if (repositoryUrl.includes('/')) {
        const [owner, repo] = repositoryUrl.split('/');
        return { owner, repo };
    }
    
    throw new Error('Invalid repository URL format. Expected: owner/repo or full GitHub URL');
}

// Firebase Function: Validate GitHub Token (Updated to handle both SDK and HTTP calls)
exports.validateGitHubToken = functions.https.onRequest((req, res) => {
    return cors(req, res, async () => {
        try {
            console.log('validateGitHubToken called with method:', req.method);
            console.log('Headers:', req.headers);
            console.log('Body:', req.body);
            
            // Handle preflight requests
            if (req.method === 'OPTIONS') {
                res.set('Access-Control-Allow-Origin', '*');
                res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
                res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
                res.status(204).send('');
                return;
            }
            
            if (req.method !== 'POST') {
                res.status(405).json({ error: 'Method not allowed' });
                return;
            }
            
            // Handle both Firebase SDK format and direct HTTP format
            let token;
            if (req.body.data && req.body.data.token) {
                // Firebase SDK format
                token = req.body.data.token;
            } else if (req.body.token) {
                // Direct HTTP format
                token = req.body.token;
            }
            
            if (!token) {
                res.status(400).json({ error: 'GitHub token is required' });
                return;
            }
            
            const result = await validateGitHubTokenInternal(token);
            console.log('Token validation result:', { valid: result.valid, user: result.user?.login });
            
            res.json({
                valid: result.valid,
                user: result.user || null,
                error: result.error || null
            });
            
        } catch (error) {
            console.error('validateGitHubToken error:', error);
            res.status(500).json({ error: error.message });
        }
    });
});

// Firebase Function: Validate GitHub Repository Access (Updated to handle both SDK and HTTP calls)
exports.validateGitHubRepository = functions.https.onRequest((req, res) => {
    return cors(req, res, async () => {
        try {
            console.log('validateGitHubRepository called with method:', req.method);
            
            // Handle preflight requests
            if (req.method === 'OPTIONS') {
                res.set('Access-Control-Allow-Origin', '*');
                res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
                res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
                res.status(204).send('');
                return;
            }
            
            if (req.method !== 'POST') {
                res.status(405).json({ error: 'Method not allowed' });
                return;
            }
            
            // Handle both Firebase SDK format and direct HTTP format
            let token, repositoryUrl;
            if (req.body.data) {
                // Firebase SDK format
                token = req.body.data.token;
                repositoryUrl = req.body.data.repositoryUrl;
            } else {
                // Direct HTTP format
                token = req.body.token;
                repositoryUrl = req.body.repositoryUrl;
            }
            
            if (!token || !repositoryUrl) {
                res.status(400).json({ error: 'GitHub token and repository URL are required' });
                return;
            }
            
            const { owner, repo } = parseRepositoryUrl(repositoryUrl);
            
            const octokit = new Octokit({
                auth: token,
            });
            
            const response = await octokit.rest.repos.get({
                owner,
                repo
            });
            
            console.log('Repository validation successful:', response.data.full_name);
            
            res.json({
                valid: true,
                repository: {
                    name: response.data.name,
                    fullName: response.data.full_name,
                    private: response.data.private,
                    permissions: response.data.permissions
                }
            });
            
        } catch (error) {
            console.error('validateGitHubRepository error:', error);
            
            if (error.status === 404) {
                res.json({
                    valid: false,
                    error: 'Repository not found or no access'
                });
                return;
            }
            
            res.status(500).json({ error: error.message });
        }
    });
});

// Firebase Function: Create GitHub Pull Request
exports.createGitHubPullRequest = functions.https.onCall(async (data, context) => {
    try {
        console.log('createGitHubPullRequest called:', { 
            repositoryUrl: data.repositoryUrl, 
            hasToken: !!data.token,
            title: data.title,
            head: data.head,
            base: data.base
        });
        
        // Validate input
        const requiredFields = ['token', 'repositoryUrl', 'title', 'body', 'head', 'base'];
        for (const field of requiredFields) {
            if (!data[field]) {
                throw new functions.https.HttpsError('invalid-argument', `${field} is required`);
            }
        }
        
        const { owner, repo } = parseRepositoryUrl(data.repositoryUrl);
        
        const octokit = new Octokit({
            auth: data.token,
        });
        
        const response = await octokit.rest.pulls.create({
            owner,
            repo,
            title: data.title,
            body: data.body,
            head: data.head,
            base: data.base
        });
        
        console.log('Pull request created successfully:', response.data.number);
        
        return {
            success: true,
            pullRequest: {
                id: response.data.id,
                number: response.data.number,
                title: response.data.title,
                body: response.data.body,
                state: response.data.state,
                htmlUrl: response.data.html_url,
                head: {
                    ref: response.data.head.ref,
                    sha: response.data.head.sha
                },
                base: {
                    ref: response.data.base.ref,
                    sha: response.data.base.sha
                },
                createdAt: response.data.created_at,
                updatedAt: response.data.updated_at
            }
        };
        
    } catch (error) {
        console.error('createGitHubPullRequest error:', error);
        
        if (error.status === 422) {
            throw new functions.https.HttpsError('invalid-argument', 'Pull request validation failed: ' + error.message);
        }
        
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Firebase Function: Get GitHub Pull Request
exports.getGitHubPullRequest = functions.https.onCall(async (data, context) => {
    try {
        console.log('getGitHubPullRequest called:', { 
            repositoryUrl: data.repositoryUrl, 
            hasToken: !!data.token,
            pullNumber: data.pullNumber
        });
        
        // Validate input
        if (!data.token || !data.repositoryUrl || !data.pullNumber) {
            throw new functions.https.HttpsError('invalid-argument', 'GitHub token, repository URL, and pull number are required');
        }
        
        const { owner, repo } = parseRepositoryUrl(data.repositoryUrl);
        
        const octokit = new Octokit({
            auth: data.token,
        });
        
        const response = await octokit.rest.pulls.get({
            owner,
            repo,
            pull_number: data.pullNumber
        });
        
        console.log('Pull request retrieved successfully:', response.data.number);
        
        return {
            success: true,
            pullRequest: {
                id: response.data.id,
                number: response.data.number,
                title: response.data.title,
                body: response.data.body,
                state: response.data.state,
                htmlUrl: response.data.html_url,
                head: {
                    ref: response.data.head.ref,
                    sha: response.data.head.sha
                },
                base: {
                    ref: response.data.base.ref,
                    sha: response.data.base.sha
                },
                createdAt: response.data.created_at,
                updatedAt: response.data.updated_at,
                merged: response.data.merged,
                mergeable: response.data.mergeable
            }
        };
        
    } catch (error) {
        console.error('getGitHubPullRequest error:', error);
        
        if (error.status === 404) {
            throw new functions.https.HttpsError('not-found', 'Pull request not found');
        }
        
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Firebase Function: Merge GitHub Pull Request
exports.mergeGitHubPullRequest = functions.https.onCall(async (data, context) => {
    try {
        console.log('mergeGitHubPullRequest called:', { 
            repositoryUrl: data.repositoryUrl, 
            hasToken: !!data.token,
            pullNumber: data.pullNumber,
            mergeMethod: data.mergeMethod
        });
        
        // Validate input
        if (!data.token || !data.repositoryUrl || !data.pullNumber) {
            throw new functions.https.HttpsError('invalid-argument', 'GitHub token, repository URL, and pull number are required');
        }
        
        const { owner, repo } = parseRepositoryUrl(data.repositoryUrl);
        const mergeMethod = data.mergeMethod || 'merge';
        
        // Validate merge method
        if (!['merge', 'squash', 'rebase'].includes(mergeMethod)) {
            throw new functions.https.HttpsError('invalid-argument', 'Invalid merge method. Must be: merge, squash, or rebase');
        }
        
        const octokit = new Octokit({
            auth: data.token,
        });
        
        const response = await octokit.rest.pulls.merge({
            owner,
            repo,
            pull_number: data.pullNumber,
            commit_title: data.commitTitle || `Merge pull request #${data.pullNumber}`,
            commit_message: data.commitMessage || '',
            merge_method: mergeMethod
        });
        
        console.log('Pull request merged successfully:', response.data.sha);
        
        return {
            success: true,
            merge: {
                sha: response.data.sha,
                merged: response.data.merged,
                message: response.data.message
            }
        };
        
    } catch (error) {
        console.error('mergeGitHubPullRequest error:', error);
        
        if (error.status === 405) {
            throw new functions.https.HttpsError('failed-precondition', 'Pull request cannot be merged: ' + error.message);
        }
        
        if (error.status === 409) {
            throw new functions.https.HttpsError('aborted', 'Merge conflict: ' + error.message);
        }
        
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Simple test function for HTTP requests
exports.testHttp = functions.https.onRequest((req, res) => {
    // Set CORS headers
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    // Handle preflight requests
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    
    console.log('testHttp called with method:', req.method);
    console.log('Body:', req.body);
    
    res.json({
        success: true,
        message: 'HTTP request successful',
        method: req.method,
        body: req.body
    });
});

