package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TsenakelyDatabase
import com.example.data.repository.ShopRepository
import com.example.ui.screens.TsenakelyApp
import com.example.ui.theme.TsenakelyTheme
import com.example.ui.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TsenakelyDatabase.getInstance(applicationContext)
        val repository = ShopRepository(database.tsenakelyDao())

        setContent {
            TsenakelyTheme {
                val shopViewModel: ShopViewModel = viewModel(
                    factory = ShopViewModel.provideFactory(repository)
                )
                TsenakelyApp(viewModel = shopViewModel)
            }
        }
    }
}
