/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import {onRequest} from "firebase-functions/v2/https";
import {logger} from "firebase-functions/v2";

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// export const helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// CORS headers for all origins
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "3600",
};

/**
 * Helper function to validate GitHub token and get user info
 * @param {string} token - GitHub personal access token
 * @return {Promise<object>} Validation result with user info or error
 */
async function validateGitHubTokenInternal(token: string) {
  try {
    const {Octokit} = await import("@octokit/rest");
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
        id: response.data.id,
      },
    };
  } catch (error: any) {
    logger.error("GitHub token validation failed:", error.message);
    return {
      valid: false,
      error: error.message,
    };
  }
}

/**
 * Helper function to parse repository URL
 * @param {string} repositoryUrl - GitHub repository URL or owner/repo format
 * @return {object} Parsed owner and repo names
 */
function parseRepositoryUrl(repositoryUrl: string) {
  // Handle both full GitHub URLs and owner/repo format
      if (repositoryUrl.includes("github.com")) {
      const match = repositoryUrl.match(/github\.com\/([^/]+)\/([^/]+)/);
      if (match) {
        return {owner: match[1], repo: match[2].replace(".git", "")};
      }
  } else if (repositoryUrl.includes("/")) {
    const [owner, repo] = repositoryUrl.split("/");
    return {owner, repo};
  }

  throw new Error(
    "Invalid repository URL format. Expected: owner/repo or full GitHub URL"
  );
}

// Validate GitHub Token
export const rocketValidateGitHubToken = onRequest(
  {cors: true},
  async (req, res) => {
    try {
      logger.info("validateGitHubToken called", {
        method: req.method,
        origin: req.headers.origin,
      });

      // Set CORS headers
      Object.entries(corsHeaders).forEach(([key, value]) => {
        res.set(key, value);
      });

      // Handle preflight requests
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }

      if (req.method !== "POST") {
        res.status(405).json({error: "Method not allowed"});
        return;
      }

      const {token} = req.body;

      if (!token) {
        res.status(400).json({error: "GitHub token is required"});
        return;
      }

      const result = await validateGitHubTokenInternal(token);
      logger.info("Token validation result", {
        valid: result.valid,
        user: result.user?.login,
      });

      res.json({
        valid: result.valid,
        user: result.user || null,
        error: result.error || null,
      });
    } catch (error: any) {
      logger.error("validateGitHubToken error:", error);
      res.status(500).json({error: error.message});
    }
  }
);

// Validate GitHub Repository
export const rocketValidateGitHubRepository = onRequest(
  {cors: true},
  async (req, res) => {
    try {
      logger.info("validateGitHubRepository called", {
        method: req.method,
        origin: req.headers.origin,
      });

      // Set CORS headers
      Object.entries(corsHeaders).forEach(([key, value]) => {
        res.set(key, value);
      });

      // Handle preflight requests
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }

      if (req.method !== "POST") {
        res.status(405).json({error: "Method not allowed"});
        return;
      }

      const {token, repositoryUrl} = req.body;

      if (!token || !repositoryUrl) {
        res.status(400).json({
          error: "GitHub token and repository URL are required",
        });
        return;
      }

      const {owner, repo} = parseRepositoryUrl(repositoryUrl);

      const {Octokit} = await import("@octokit/rest");
      const octokit = new Octokit({
        auth: token,
      });

      const response = await octokit.rest.repos.get({
        owner,
        repo,
      });

      logger.info("Repository validation successful", {
        fullName: response.data.full_name,
      });

      res.json({
        valid: true,
        repository: {
          name: response.data.name,
          fullName: response.data.full_name,
          private: response.data.private,
          permissions: response.data.permissions,
        },
      });
    } catch (error: any) {
      logger.error("validateGitHubRepository error:", error);

      if (error.status === 404) {
        res.json({
          valid: false,
          error: "Repository not found or no access",
        });
        return;
      }

      res.status(500).json({error: error.message});
    }
  }
);
