package com.rapido.rocket

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.COMPLETE
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

@JsName("console")
private external object Console {
    fun log(message: String)
    fun error(message: String)
    fun error(message: String, error: JsAny = definedExternally)
}

// External declarations for window functions
private external interface Event : JsAny
private external interface Window {
    fun addEventListener(type: String, listener: (Event) -> Unit)
}

@JsName("window")
private external val window: Window

private external fun checkFirebaseInitialized(): Boolean = definedExternally
private external fun showApp(): Unit = definedExternally

// Implementation of external functions
private fun checkFirebaseInitializedImpl(): Boolean =
    js("typeof window.firebaseInitialized !== 'undefined' && window.firebaseInitialized === true")

// Enable inspection
private fun enableInspection() {
    js("""
        if (typeof window.__COMPOSE_INSPECTOR_GLOBAL_HOOK__ === 'undefined') {
            window.__COMPOSE_INSPECTOR_GLOBAL_HOOK__ = {
                isDisabled: false,
                componentsById: new Map(),
                components: [],
                fiber: null
            };
        }
    """)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Console.log("Starting app initialization...")
    
    try {
        Console.log("Setting up window load handler...")
        enableInspection()
        


        // Check if document is already loaded
        if (document.readyState == DocumentReadyState.COMPLETE) {
            Console.log("Document already loaded, initializing immediately...")
            initializeApp()
        } else {
            Console.log("Document not ready, setting up load listener...")
            // Use only DOMContentLoaded to avoid duplicate calls
            window.addEventListener("DOMContentLoaded", { 
                Console.log("DOMContentLoaded event fired")
                initializeApp() 
            })
        }
    } catch (e: Throwable) {
        Console.error("Failed in main initialization", e as JsAny)
        document.body?.innerHTML = "Failed in main initialization. Please check console for details."
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun initializeApp() {
    Console.log("Initializing app...")
    
    try {
        Console.log("Checking Firebase initialization status...")
        val firebaseInitialized = checkFirebaseInitializedImpl()
        Console.log("Firebase initialized: $firebaseInitialized")
        
        if (!firebaseInitialized) {
            Console.log("Firebase not initialized yet, retrying...")
            // Use a simple timeout to retry
            kotlinx.browser.window.setTimeout({
                initializeApp()
                null // Return null to satisfy JsAny? requirement
            }, 100)
            return
        }

        Console.log("Looking for root element...")
        val rootElement = document.getElementById("root")
        if (rootElement == null) {
            Console.error("Root element not found!")
            document.body?.innerHTML = "Failed to initialize app: Root element not found"
            return
        }
        Console.log("Root element found: $rootElement")

        Console.log("Setting up ComposeViewport...")
        ComposeViewport(rootElement) {
            Console.log("Rendering App composable...")
            App()
        }
        Console.log("ComposeViewport setup complete")
        
        // Call the JavaScript function to show the app
        showApp()
    } catch (e: Throwable) {
        Console.error("Failed to initialize app: ${e.message}")
        Console.error("Error details: ${e.stackTraceToString()}")
        Console.error("Full error object", e as JsAny)
        document.body?.innerHTML = """
            <div style="padding: 20px; background: #ff4444; color: white; font-family: Arial;">
                <h3>Failed to initialize app</h3>
                <p>Error: ${e.message ?: "Unknown error"}</p>
                <p>Please check console for details.</p>
                <details>
                    <summary>Stack trace</summary>
                    <pre style="background: #333; padding: 10px; overflow: auto;">${e.stackTraceToString()}</pre>
                </details>
            </div>
        """.trimIndent()
    }
} 