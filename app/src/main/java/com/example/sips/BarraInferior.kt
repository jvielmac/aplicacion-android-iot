package com.example.sips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun NavBar() {
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        NavigationBarItem(
            selected = true,
            icon = { Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = "Estado Actual"
            )},
            label = { Text("Estado Actual") },
            onClick = {},
        )

        NavigationBarItem(
            selected = false,
            icon = { Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "Alertas"
            )},
            label = { Text("Alertas") },
            onClick = {},
        )
    }
}