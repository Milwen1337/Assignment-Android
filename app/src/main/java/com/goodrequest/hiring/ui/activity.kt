package com.goodrequest.hiring.ui

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.databinding.ActivityBinding

class PokemonActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
        vm.load()

        ActivityBinding.inflate(layoutInflater).run {
            setContentView(root)
            refresh.setOnRefreshListener { vm.load() }
            retry.setOnClickListener { vm.load() }

            vm.pokemons.observe(this@PokemonActivity) { result: Result<List<Pokemon>>? ->
                result?.fold(
                    onSuccess = { pokemons ->
                        loading.visibility = GONE
                        val adapter = PokemonAdapter()
                        items.adapter = adapter
                        adapter.show(pokemons)
                    },
                    onFailure = {
                        loading.visibility = GONE
                        failure.visibility = VISIBLE
                    }
                )
            }
        }
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