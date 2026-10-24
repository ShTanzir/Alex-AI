package com.alexai.assistant.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.alexai.assistant.commands.ActionResult;

/**
 * Optional, user-enabled service for three narrow actions: Back, Recents and scrolling.
 * It ignores all accessibility events, never reads or stores screen text, and only looks at whether a
 * node is scrollable. Android lets the user switch it off at any time.
 */
public class AlexAccessibilityService extends AccessibilityService {
    public static final String OFF_MESSAGE =
            "The Alex accessibility service is off. Turn it on from the Settings tab (Accessibility), then try again.";

    private static volatile AlexAccessibilityService instance;

    public static boolean isRunning() { return instance != null; }

    public static ActionResult back() { return global(GLOBAL_ACTION_BACK, "Pressed Back."); }

    public static ActionResult recents() { return global(GLOBAL_ACTION_RECENTS, "Opened recent apps."); }

    private static ActionResult global(int action, String ok) {
        AlexAccessibilityService s = instance;
        if (s == null) return ActionResult.fail(OFF_MESSAGE);
        return s.performGlobalAction(action) ? ActionResult.ok(ok)
                : ActionResult.fail("Android didn't accept that action.");
    }

    public static ActionResult scroll(boolean down) {
        AlexAccessibilityService s = instance;
        if (s == null) return ActionResult.fail(OFF_MESSAGE);
        AccessibilityNodeInfo root = s.getRootInActiveWindow();
        if (root == null) return ActionResult.fail("I can't see the current screen (some apps block this).");
        AccessibilityNodeInfo target = findScrollable(root);
        if (target == null) return ActionResult.fail("I couldn't find anything scrollable on this screen.");
        boolean done = target.performAction(down
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        return done ? ActionResult.ok("Scrolled " + (down ? "down" : "up") + ".")
                : ActionResult.fail("That screen didn't scroll.");
    }

    private static AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findScrollable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* intentionally ignored */ }

    @Override public void onInterrupt() {}
}
