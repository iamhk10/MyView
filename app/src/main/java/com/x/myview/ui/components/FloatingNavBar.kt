package com.x.myview.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.ui.theme.LocalIsDark
import com.x.myview.ui.theme.LocalGlass

val NAV_BAR_HEIGHT = 68.dp

class NavTab(val label: String, val icon: ImageVector)

/** Floating tab bar. A pill slides under the selected tab and its icon pops. */
@Composable
fun FloatingNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(NAV_BAR_HEIGHT),
        shape = RoundedCornerShape(32.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / tabs.size
            val pillX by animateDpAsState(
                itemWidth * selected,
                spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "pill",
            )
            Box(
                Modifier
                    .offset(x = pillX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
            )
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { i, tab ->
                    NavItem(
                        tab,
                        i == selected,
                        {
                            if (i != selected) haptic()
                            onSelect(i)
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        if (selected) scheme.primary else scheme.onSurfaceVariant,
        tween(200),
        label = "navColor",
    )
    val pop by animateFloatAsState(
        if (selected) 1.18f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navPop",
    )
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
        )
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}


/** Height of the Bento dock plus its raised centre button and the gap under it. */
val BENTO_DOCK_SPACE = 100.dp

/**
 * Bento look: a floating glass pill with four icons (no labels, a small glowing dot under the active
 * one) and a raised round "+" button in the middle. [selected] may be -1 when none of the four is open.
 */
@Composable
fun BentoDock(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic(strong = true)
    val tickHaptic = rememberHaptic()
    val scheme = MaterialTheme.colorScheme
    val half = tabs.size / 2
    Box(modifier.widthIn(max = 380.dp).fillMaxWidth().height(NAV_BAR_HEIGHT + 12.dp)) {
        GlassSurface(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(NAV_BAR_HEIGHT - 4.dp),
            shape = RoundedCornerShape(999.dp),
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { i, tab ->
                    if (i == half) Spacer(Modifier.width(64.dp))
                    DockItem(
                        tab,
                        i == selected,
                        {
                            if (i != selected) tickHaptic()
                            onSelect(i)
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .shadow(14.dp, CircleShape, clip = false, ambientColor = scheme.primary, spotColor = scheme.primary)
                .background(scheme.primary, CircleShape)
                .border(3.dp, scheme.background, CircleShape)
                .clip(CircleShape)
                .bounceClick {
                    haptic()
                    onAdd()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Quick add", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun DockItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        if (selected) scheme.primary else scheme.onSurfaceVariant,
        tween(220),
        label = "dockColor",
    )
    val dot by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dockDot",
    )
    Column(
        modifier
            .fillMaxHeight()
            .bounceClick(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .size(5.dp)
                .graphicsLayer {
                    scaleX = dot
                    scaleY = dot
                    alpha = dot
                }
                .shadow(6.dp, CircleShape, clip = false, ambientColor = scheme.primary, spotColor = scheme.primary)
                .background(scheme.primary, CircleShape),
        )
    }
}


/** Height of the Health bottom bar (without the system navigation inset). */
val HEALTH_BAR_HEIGHT = 72.dp

/** Space the content leaves at the bottom of Health tabs: the bar plus the Quick add button above it. */
val HEALTH_BOTTOM_SPACE = 148.dp

/**
 * Health look: a short, full-width bottom bar (like Google Health). The selected tab gets a pill behind
 * its icon that grows in with a spring, and its label turns bold. [selected] is the index in [tabs].
 */
@Composable
fun HealthNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val glass = LocalGlass.current
    val dark = LocalIsDark.current
    val scheme = MaterialTheme.colorScheme
    val barShape = RoundedCornerShape(34.dp)
    // Liquid glass: a floating capsule that blurs what scrolls behind it. Otherwise a plain full-width bar.
    val barModifier = if (glass) {
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .fillMaxWidth()
            .shadow(18.dp, barShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
            .liquidGlass(barShape, scheme.surfaceContainer.copy(alpha = if (dark) 0.45f else 0.50f), blur = 24.dp)
    } else {
        modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer)
    }
    Box(barModifier) {
        Row(
            (if (glass) Modifier else Modifier.navigationBarsPadding())
                .fillMaxWidth()
                .height(if (glass) HEALTH_BAR_HEIGHT - 4.dp else HEALTH_BAR_HEIGHT)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                HealthNavItem(
                    tab,
                    i == selected,
                    {
                        if (i != selected) haptic()
                        onSelect(i)
                    },
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HealthNavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val pillWidth by animateDpAsState(
        if (selected) 64.dp else 32.dp,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "healthPill",
    )
    val pillColor by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.primaryContainer.copy(alpha = 0f),
        tween(200),
        label = "healthPillColor",
    )
    Column(
        modifier
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(pillWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Extended floating action button with a rounded-square shape (Material 3 Expressive style). */
@Composable
fun HealthFab(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptic(strong = true)
    val shape = RoundedCornerShape(20.dp)
    val body = if (LocalGlass.current) {
        modifier
            .shadow(14.dp, shape, clip = false, ambientColor = scheme.primary, spotColor = scheme.primary)
            .liquidGlass(shape, scheme.primary.copy(alpha = 0.78f), blur = 16.dp)
    } else {
        modifier
            .shadow(6.dp, shape, clip = false)
            .background(scheme.primary, shape)
            .clip(shape)
    }
    Row(
        body
            .bounceClick {
                haptic()
                onClick()
            }
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
    }
}
