package com.goodrequest.hiring.ui

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.databinding.ActivityBinding
import com.google.android.material.snackbar.Snackbar

class PokemonActivity: ComponentActivity() {
    private val adapter = PokemonAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Info Log", "onCreate")
        val isNewInstance = savedInstanceState == null
        val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
        if (isNewInstance) {
            vm.load(LoadState.LoadingFirst)
        }

        ActivityBinding.inflate(layoutInflater).run {
            setContentView(root)
            refresh.setOnRefreshListener { vm.load(LoadState.Refreshing) }
            retry.setOnClickListener { vm.load(LoadState.LoadingFirst) }
            items.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount - 1
                    val lastCompletelyVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    if (lastCompletelyVisiblePosition == totalItemCount && !adapter.hasLoading()) {
                        adapter.addLoading{ vm.load(LoadState.LoadingOthers) }
                        vm.load(LoadState.LoadingOthers)
                    }
                }
            })

            vm.pokemons.observe(this@PokemonActivity) { result: Result<List<Pokemon>>? ->
                result?.fold(
                    onSuccess = { pokemons ->
                        Log.i("Info Log", "onSuccess: ${pokemons.size}")
                        val loadState = vm.loadState.value
                        if (loadState == LoadState.LoadingFirst || loadState == LoadState.Refreshing){
                            Log.i("Info Log", "onSuccess - loadingFirst or Refreshing")
                            vm.lastPokemonData.postValue(pokemons)
                        } else if (loadState == LoadState.LoadingOthers){
                            Log.i("Info Log", "onSuccess - loadingOthers")
                            val oldData = vm.lastPokemonData.value.orEmpty()
                            vm.lastPokemonData.postValue(oldData + pokemons)
                        }
                        loading.visibility = GONE
                        failure.visibility = GONE
                        refresh.isRefreshing = false
                    },
                    onFailure = {
                        Log.i("Info Log", "onError: ${vm.loadState.value}, adapter: isNull = ${items.adapter == null}")
                        val lastData = vm.lastPokemonData.value
                        if (vm.loadState.value == LoadState.Refreshing){
                            showErrorSnackbar(root, "Error occurred while refreshing pokemon data.")
                            if (items.adapter == null && lastData != null){
                                showPokemons(items, lastData)
                            }
                        } else if (vm.loadState.value == LoadState.LoadingOthers){
                            adapter.showRetry()
                        }

                        failure.visibility = if (vm.loadState.value == LoadState.LoadingFirst && items.adapter == null) VISIBLE else GONE
                        loading.visibility = GONE
                        refresh.isRefreshing = false
                    }
                )
            }

            vm.lastPokemonData.observe(this@PokemonActivity) { lastData: List<Pokemon>? ->
                lastData?.let {
                    Log.i("Info Log", "LiveData observer: lastData size: ${lastData.size}")
                    adapter.removeLoading()
                    if (vm.loadState.value == LoadState.Refreshing){
                        refreshPokemons(items, vm.pokemons.value?.getOrNull().orEmpty())
                    } else {
                        showPokemons(items, vm.pokemons.value?.getOrNull().orEmpty())
                    }
                    vm.loadState.postValue(LoadState.Loaded)
                }
            }
        }
    }

    private fun showPokemons(items: RecyclerView, data: List<Pokemon>){
        if (items.adapter == null) items.adapter = adapter
        adapter.show(data)
    }

    private fun refreshPokemons(items: RecyclerView, data: List<Pokemon>){
        if (items.adapter == null) items.adapter = adapter
        adapter.refresh(data)
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