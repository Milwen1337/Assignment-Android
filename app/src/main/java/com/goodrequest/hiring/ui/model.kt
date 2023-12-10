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
    val loadState = state.getLiveData<LoadState>("loadState", LoadState.Loading)

    fun load(mLoadState: LoadState, mockError: Boolean = false) {
        Log.i("Info Log", "load")
        loadState.postValue(mLoadState)
        GlobalScope.launch {
            val result = if (mockError)api.getPokemonsMockError() else api.getPokemons(page = 1)
            Log.i("Info Log", "load - get data")
            pokemons.postValue(result)
            if (result.isSuccess) lastPokemonData.postValue(result.getOrNull())
        }
    }
}
@Parcelize
data class Pokemon(
    val id     : String,
    val name   : String,
    val detail : PokemonDetail? = null): Parcelable

@Parcelize
data class PokemonDetail(
    val image  : String,
    val move   : String,
    val weight : Int): Parcelable

sealed class LoadState: Parcelable {
    @Parcelize
    data object Refreshing: LoadState()
    @Parcelize
    data object Loading: LoadState()
    @Parcelize
    data object Loaded: LoadState()
}