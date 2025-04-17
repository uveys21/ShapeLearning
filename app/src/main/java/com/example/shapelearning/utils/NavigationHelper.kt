package com.example.shapelearning.utils

import androidx.navigation.NavController
import com.example.shapelearning.R

class NavigationHelper {\n\n    fun navigateToGameModeSelection(navController: NavController) {\n        // Assuming this action exists in your nav_graph.xml.  If not, adjust accordingly.\n        navController.navigate(R.id.action_mainMenuFragment_to_gameModeSelectionFragment)\n    }\n\n    fun navigateToSettings(navController: NavController) {\n        // Assuming this action exists in your nav_graph.xml. If not, adjust accordingly.\n        navController.navigate(R.id.action_mainMenuFragment_to_settingsFragment)\n    }\n\n    fun navigateToParentPin(navController: NavController) {\n        // Assuming this action exists in your nav_graph.xml. If not, adjust accordingly.\n        navController.navigate(R.id.action_mainMenuFragment_to_parentPinFragment)\n    }\n\n    fun navigateUp(navController: NavController) {\n        navController.navigateUp()\n    }\n}\n
    }
}