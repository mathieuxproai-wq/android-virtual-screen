package io.github.lejw0925.virtualscreen

import android.app.Application
import android.graphics.Rect
import io.github.lejw0925.virtualscreen.backend.NodeInfo
import io.github.lejw0925.virtualscreen.backend.NodeSnapshot
import io.github.lejw0925.virtualscreen.backend.WindowSnapshot
import io.github.lejw0925.virtualscreen.mcp.NodeTreeFormatter
import io.github.lejw0925.virtualscreen.mcp.SettleSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettleAndBudgetTest {
    @Test fun settleSignatureIgnoresNodeIdsAndTracksLayoutChanges() {
        val first = listOf(
            NodeInfo("a", text = "Cart", bounds = Rect(0, 0, 100, 50), windowId = 7, clickable = true),
            NodeInfo("b", text = "Total", bounds = Rect(0, 60, 100, 90), windowId = 7),
        )
        val sameContentNewIds = listOf(
            NodeInfo("x", text = "Cart", bounds = Rect(0, 0, 100, 50), windowId = 7, clickable = true),
            NodeInfo("y", text = "Total", bounds = Rect(0, 60, 100, 90), windowId = 7),
        )
        val moved = listOf(
            NodeInfo("a", text = "Cart", bounds = Rect(0, 10, 100, 50), windowId = 7, clickable = true),
            NodeInfo("b", text = "Total", bounds = Rect(0, 60, 100, 90), windowId = 7),
        )

        assertEquals(SettleSignature.of(first), SettleSignature.of(sameContentNewIds))
        assertNotEquals(SettleSignature.of(first), SettleSignature.of(moved))
        assertEquals("", SettleSignature.of(emptyList()))
    }

    @Test fun compactBudgetPrefersInteractiveNodesAndReportsWhatWasDropped() {
        val nodes = buildList {
            add(NodeInfo("root", windowId = 7))
            repeat(30) { index ->
                add(NodeInfo("text-$index", text = "Readable $index", parentId = "root", windowId = 7,
                    bounds = Rect(0, index * 10, 100, index * 10 + 8)))
            }
            repeat(5) { index ->
                add(NodeInfo("button-$index", text = "Action $index", parentId = "root", windowId = 7,
                    clickable = true, bounds = Rect(0, index * 20, 100, index * 20 + 18)))
            }
        }
        val snapshot = NodeSnapshot(
            observationId = "observation",
            displayId = 7,
            packageName = "fixture",
            nodes = nodes,
            windows = listOf(WindowSnapshot(7, 7, 1, "fixture", "Fixture", 4, focused = true, active = true)),
        )

        val compact = NodeTreeFormatter.values(snapshot, "compact", maxNodes = 10)
        val kept = compact["nodes"] as List<Map<String, Any?>>
        val keptIds = kept.map { it.getValue("id") as String }

        assertTrue(kept.size <= 10)
        assertEquals(nodes.count { it.clickable }, keptIds.count { it.startsWith("button-") })
        assertEquals((compact["returned_node_count"] as Int), kept.size)
        assertTrue((compact["pruned_node_count"] as Int) > 0)
        assertEquals(10, compact["max_nodes"])
        assertTrue(kept.any { it.containsKey("pos") })

        val full = NodeTreeFormatter.values(snapshot, "full", maxNodes = 10)
        assertEquals(nodes.size, (full["nodes"] as List<*>).size)
        assertNull(full["max_nodes"])
        assertEquals(0, full["pruned_node_count"])
    }
}
