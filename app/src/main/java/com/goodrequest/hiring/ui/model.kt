package com.goodrequest.hiring.ui

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.goodrequest.hiring.PokemonApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class PokemonViewModel(
    state: SavedStateHandle,
    private val context: Context?,
    private val api: PokemonApi) : ViewModel() {

    val lastPokemonData = state.getLiveData<List<Pokemon>?>("lastPokemons", null)
    val pokemons = state.getLiveData<Result<List<Pokemon>>?>("pokemons", null)
    val loadState = state.getLiveData<LoadState>("loadState", LoadState.LoadingFirst)
    val lastLoadedPage = state.getLiveData("loadedPage", 0)
    fun load(mLoadState: LoadState, mockError: Boolean = false) {
        Log.i("PokemonViewModel", "load data")
        loadState.postValue(mLoadState)
        GlobalScope.launch {
            Log.i("PokemonViewModel", "load - get data")
            val page = (lastLoadedPage.value?:0) + 1
            Log.i("PokemonViewModel", "load - get data for page $page")
            val result = if (mockError){
                api.getPokemonsMockError()
            } else {
                api.getPokemons(page = page)
            }
            Log.i("PokemonViewModel", "load - get data")
            if (result.isSuccess){
                result.getOrNull()?.let { pokemonList ->
                    Log.i("PokemonViewModel", "load - get details")
                    pokemonList.forEach { pokemon ->
                        Log.i("PokemonViewModel", "load - get details: ${pokemon.name}")
                        val pokemonDetail = api.getPokemonDetail(pokemon)
                        pokemon.detail = pokemonDetail.getOrNull()
                    }
                }
                Log.i("PokemonViewModel", "load - post details")
                lastLoadedPage.postValue(page)
            }
            Log.i("PokemonViewModel", "load - post data")
            pokemons.postValue(result)
        }
    }
}
@Parcelize
data class Pokemon(
    val id     : String,
    val name   : String,
    var detail : PokemonDetail? = null): Parcelable

@Parcelize
data class PokemonDetail(
    val image  : String,
    val move   : String,
    val weight : Int): Parcelable

sealed class LoadState: Parcelable {
    @Parcelize
    data object Refreshing: LoadState()
    @Parcelize
    data object LoadingFirst: LoadState()
    @Parcelize
    data object LoadingOthers: LoadState()
    @Parcelize
    data object Loaded: LoadState()
}