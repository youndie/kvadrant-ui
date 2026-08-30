package io.github.youndie.kvadrant.sample.ios

import androidx.compose.ui.window.ComposeUIViewController
import io.github.youndie.kvadrant.sample.KvadrantSampleApp
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow

/**
 * The demo on iOS, and the whole of what "the part with no Kotlin in it" turned out to be.
 *
 * An iOS application needs a `UIApplicationMain`, a delegate that owns a window, and a root view
 * controller. `platform.UIKit` has all three, so this is built by the same compiler out of the same
 * source set as everything it draws, and there is no `.pbxproj` in this repository.
 *
 * It exists because [B-07](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-07-font-stack.md)
 * could not be closed without a target and D14 only grants one when something runs on it. The
 * assertion that closes the item is `IosFontStackTest`, which Gradle runs on a simulator inside
 * `check`; this is the other half — the same `KvadrantSampleApp` the desktop opens and the Android
 * demo hosts, on the third renderer, so that a person can look at it.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public fun main() {
    memScoped {
        val args = arrayOf("KvadrantSample")
        UIApplicationMain(
            argc = args.size,
            argv = args.map { it.cstr.ptr }.toCValues().ptr,
            principalClassName = null,
            // The delegate by name, which is how UIKit finds it without a storyboard. A misspelling
            // is a black screen and no error: UIKit proceeds without a delegate rather than
            // complaining that it has none.
            delegateClassName = NSStringFromClass(KvadrantSampleDelegate),
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class KvadrantSampleDelegate :
    UIResponder,
    UIApplicationDelegateProtocol {
    /**
     * **UIKit creates this class itself**, with `[[Class alloc] init]`, and a Kotlin class exports
     * no such initialiser by default. Without this the application launches and dies with
     * "Initializer is not implemented" *before any Kotlin of ours runs* — so nothing in the app can
     * report it and the only trace is in the device log.
     */
    @OverrideInit
    public constructor() : super()

    public companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta

    // Backed by a field of another name: `window` is the protocol's property, so a private one
    // spelled the same way hides it instead of implementing it.
    private var held: UIWindow? = null

    override fun window(): UIWindow? = held

    override fun setWindow(window: UIWindow?) {
        held = window
    }

    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?,
    ): Boolean {
        held =
            UIWindow(frame = UIScreen.mainScreen.bounds).apply {
                rootViewController = ComposeUIViewController { KvadrantSampleApp() }
                makeKeyAndVisible()
            }
        return true
    }
}
