package com.healthoracle.presentation.walktracker

import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.healthoracle.data.model.RoutePoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    routePoints: List<RoutePoint>,
    currentLat: Double? = null,
    currentLng: Double? = null,
    showStartEndMarkers: Boolean = false,
    startPoint: RoutePoint? = null,
    endPoint: RoutePoint? = null
) {
    val mapView = rememberMapViewWithLifecycle()

    LaunchedEffect(routePoints, currentLat, currentLng) {
        mapView.overlays.clear()

        if (routePoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor("#00E5FF")
                outlinePaint.strokeWidth = 12f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
            }
            mapView.overlays.add(polyline)

            // Start marker (Neon Green Dot)
            if (showStartEndMarkers && startPoint != null) {
                val startMarker = Marker(mapView).apply {
                    position = GeoPoint(startPoint.latitude, startPoint.longitude)
                    // Anchor to the exact center of the circle
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Start"
                    icon = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(android.graphics.Color.parseColor("#00FF87")) // Neon Green
                        setStroke(6, android.graphics.Color.parseColor("#1A1A1A")) // Dark border
                        setSize(40, 40)
                    }
                }
                mapView.overlays.add(startMarker)
            }

            // End marker or current location marker (Neon Cyan Puck)
            val targetPoint = if (showStartEndMarkers) endPoint else
                if (currentLat != null && currentLng != null) RoutePoint(currentLat, currentLng, 0) else null

            if (targetPoint != null) {
                val currentMarker = Marker(mapView).apply {
                    position = GeoPoint(targetPoint.latitude, targetPoint.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = if (showStartEndMarkers) "End" else "You are here"
                    icon = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(android.graphics.Color.parseColor("#00E5FF")) // Neon Cyan
                        setStroke(8, android.graphics.Color.parseColor("#FFFFFF")) // White border
                        setSize(48, 48)
                    }
                }
                mapView.overlays.add(currentMarker)
            }

            // Center map on route
            val centerLat = routePoints.map { it.latitude }.average()
            val centerLng = routePoints.map { it.longitude }.average()
            mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
            mapView.controller.setZoom(16.0)
        } else if (currentLat != null && currentLng != null) {
            // Initial location marker before walking starts
            val marker = Marker(mapView).apply {
                position = GeoPoint(currentLat, currentLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "You are here"
                icon = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#00E5FF"))
                    setStroke(8, android.graphics.Color.parseColor("#FFFFFF"))
                    setSize(48, 48)
                }
            }
            mapView.overlays.add(marker)
            mapView.controller.animateTo(GeoPoint(currentLat, currentLng))
            mapView.controller.setZoom(17.0)
        }

        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = androidx.compose.ui.platform.LocalContext.current
    Configuration.getInstance().userAgentValue = context.packageName

    // Define a modern Dark Mode tile source from CARTO
    val modernDarkTileSource = remember {
        XYTileSource(
            "CartoDbDarkMatter",
            0, 20, 256, ".png", arrayOf(
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/"
            ), "© OpenStreetMap contributors, © CARTO"
        )
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(modernDarkTileSource)
            setMultiTouchControls(true)
            // Removes the ugly grey grid background while tiles load
            overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.parseColor("#242424")
            controller.setZoom(16.0)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    return mapView
}