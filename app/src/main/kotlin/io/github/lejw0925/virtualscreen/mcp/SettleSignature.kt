package io.github.lejw0925.virtualscreen.mcp

import io.github.lejw0925.virtualscreen.backend.NodeInfo
import java.security.MessageDigest

/**
 * Structural signature of a collected tree, used to decide when the UI has settled
 * after an input. Deliberately ignores node ids and observation ids, which change on
 * every collection, and ignores purely visual state that does not move the layout.
 */
internal object SettleSignature {
    fun of(nodes: List<NodeInfo>): String {
        if (nodes.isEmpty()) return ""
        val builder = StringBuilder(nodes.size * 28)
        for (node in nodes) {
            builder.append(node.className.orEmpty()).append('|')
                .append(node.text.orEmpty()).append('|')
                .append(node.contentDescription.orEmpty()).append('|')
                .append(node.hintText.orEmpty()).append('|')
                .append(node.stateDescription.orEmpty()).append('|')
                .append(if (node.visible) '1' else '0')
                .append(node.scrollable).append(node.editable).append(node.enabled).append('|')
                .append(node.bounds?.let { "${it.left},${it.top},${it.right},${it.bottom}" }.orEmpty())
                .append(';')
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(builder.toString().toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(digest.size * 2)
        for (byte in digest) hex.append(String.format("%02x", byte.toInt() and 0xff))
        return hex.toString()
    }
}
