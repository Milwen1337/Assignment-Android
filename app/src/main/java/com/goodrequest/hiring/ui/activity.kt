package com.goodrequest.hiring.ui

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.databinding.ActivityBinding
import com.google.android.material.snackbar.Snackbar

class PokemonActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Info Log", "onCreate")
        val isNewInstance = savedInstanceState == null
        val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
        if (isNewInstance) {
            vm.load(LoadState.Loading)
        }

        ActivityBinding.inflate(layoutInflater).run {
            setContentView(root)
            refresh.setOnRefreshListener { vm.load(LoadState.Refreshing) }
            retry.setOnClickListener { vm.load(LoadState.Loading) }

            vm.pokemons.observe(this@PokemonActivity) { result: Result<List<Pokemon>>? ->
                result?.fold(
                    onSuccess = { pokemons ->
                        Log.i("Info Log", "onSuccess")
                        loading.visibility = GONE
                        failure.visibility = GONE
                        refresh.isRefreshing = false
                        showPokemons(items, pokemons)
                        vm.loadState.postValue(LoadState.Loaded)
                    },
                    onFailure = {
                        Log.i("Info Log", "onError: ${vm.loadState.value}, adapter: isNull = ${items.adapter == null}")
                        val lastData = vm.lastPokemonData.value
                        if (vm.loadState.value == LoadState.Refreshing){
                            showErrorSnackbar(root, "Error occurred while refreshing pokemon data.")
                            if (items.adapter == null && lastData != null){
                                showPokemons(items, lastData)
                            }
                        }

                        failure.visibility = if (vm.loadState.value == LoadState.Loading && items.adapter == null) VISIBLE else GONE
                        loading.visibility = GONE
                        refresh.isRefreshing = false
                    }
                )
            }
        }
    }

    private fun showPokemons(items: RecyclerView, data: List<Pokemon>){
        val adapter = PokemonAdapter()
        items.adapter = adapter
        adapter.show(data)
    }

    private fun showErrorSnackbar(view: ViewGroup, msg: String){
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
    }
}

/**
 * Helper function that enables us to directly call constructor of our ViewModel but also
 * provides access to SavedStateHandle.
 * Shit like this is usually generated by Hilt
 */
inline fun <reified VM: ViewModel> ComponentActivity.viewModel(crossinline create: (SavedStateHandle) -> VM) =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = {
            object: AbstractSavedStateViewModelFactory(this@viewModel, null) {
                override fun <T : ViewModel> create(key: String, type: Class<T>, handle: SavedStateHandle): T =
                    create(handle) as T
            }
    })