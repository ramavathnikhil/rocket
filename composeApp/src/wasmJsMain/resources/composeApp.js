const firebaseConfig = {
    apiKey: "AIzaSyBp2Xzont2ixx7hmYcJ1L0_BJQ8bjAUtQc",
    authDomain: "rapido-captain-test.firebaseapp.com",
    databaseURL: "https://rapido-captain-test.firebaseio.com",
    projectId: "rapido-captain-test",
    storageBucket: "rapido-captain-test.firebasestorage.app",
    messagingSenderId: "301187883692",
    appId: "1:301187883692:web:dc6fa5c806c9e4e821f509"
};

window.firebaseConfig = firebaseConfig;

// GitHub API functions for WASM
window.createGitHubPR = async function(repositoryUrl, token, title, body, head, base) {
    try {
        const response = await fetch(`https://api.github.com/repos/${repositoryUrl}/pulls`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/vnd.github.v3+json',
                'Content-Type': 'application/json',
                'User-Agent': 'Rapido-Rocket-App'
            },
            body: JSON.stringify({
                title: title,
                body: body,
                head: head,
                base: base
            })
        });
        
        if (!response.ok) {
            throw new Error(`GitHub API error: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('GitHub API error:', error);
        throw error;
    }
};

window.getGitHubPR = async function(repositoryUrl, token, pullNumber) {
    try {
        const response = await fetch(`https://api.github.com/repos/${repositoryUrl}/pulls/${pullNumber}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'Rapido-Rocket-App'
            }
        });
        
        if (!response.ok) {
            throw new Error(`GitHub API error: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('GitHub API error:', error);
        throw error;
    }
};

window.mergeGitHubPR = async function(repositoryUrl, token, pullNumber, mergeMethod) {
    try {
        const response = await fetch(`https://api.github.com/repos/${repositoryUrl}/pulls/${pullNumber}/merge`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/vnd.github.v3+json',
                'Content-Type': 'application/json',
                'User-Agent': 'Rapido-Rocket-App'
            },
            body: JSON.stringify({
                commit_title: `Merge pull request #${pullNumber}`,
                merge_method: mergeMethod
            })
        });
        
        if (!response.ok) {
            throw new Error(`GitHub API error: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('GitHub API error:', error);
        throw error;
    }
};

window.validateGitHubToken = async function(token) {
    try {
        const response = await fetch('https://api.github.com/user', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'Rapido-Rocket-App'
            }
        });
        
        return response.ok;
    } catch (error) {
        console.error('GitHub token validation error:', error);
        return false;
    }
};

window.validateGitHubRepo = async function(repositoryUrl, token) {
    try {
        const response = await fetch(`https://api.github.com/repos/${repositoryUrl}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'Rapido-Rocket-App'
            }
        });
        
        return response.ok;
    } catch (error) {
        console.error('GitHub repository validation error:', error);
        return false;
    }
}; 