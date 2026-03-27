package com.healthoracle.presentation.walktracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.healthoracle.data.model.RoutePoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    routePoints: List<RoutePoint>,
    currentLat: Double? = null,
    currentLng: Double? = null
) {
    val mapView = rememberMapViewWithLifecycle()

    LaunchedEffect(routePoints, currentLat, currentLng) {
        mapView.overlays.clear()

        if (routePoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = android.graphics.Color.parseColor("#01696f")
                outlinePaint.strokeWidth = 8f
            }
            mapView.overlays.add(polyline)
        }

        if (currentLat != null && currentLng != null) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(currentLat, currentLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "You are here"
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

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    return mapView
}
