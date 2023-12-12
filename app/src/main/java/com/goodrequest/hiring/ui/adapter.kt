package com.goodrequest.hiring.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.goodrequest.hiring.R
import com.goodrequest.hiring.databinding.ItemBinding
import com.goodrequest.hiring.databinding.LoaderBinding

class PokemonAdapter: RecyclerView.Adapter<ItemViewHolder>() {
    private val items = ArrayList<AdapterItem>()
    override fun getItemViewType(position: Int): Int {
        return items[position].type.type
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return when(viewType){
            ViewItemType.POKEMON.type -> {
                PokemonItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
                )
            }
            else -> {
                LoaderItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.loader, parent, false)
                )
            }
        }
    }
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        when(item.type){
            ViewItemType.POKEMON -> {
                (holder as? PokemonItemViewHolder)?.show(item as PokemonItem)
            }
            else -> {
                (holder as? LoaderItemViewHolder)?.showLoader(item as LoaderItem)
            }
        }
    }
    override fun getItemCount(): Int =
        items.size

    fun show(pokemons: List<Pokemon>) {
        Log.i("PokemonAdapter", "show pokemons: ${pokemons.size}")
        val adapterItems = pokemons.map { PokemonItem(it) }

        val lastPos = items.size
        items.addAll(adapterItems)
        notifyItemRangeInserted(lastPos, adapterItems.size)
    }
    fun addLoading(onRetry: () -> Unit){
        Log.i("PokemonAdapter", "add loading")
        if (!hasLoading()){
            val lastPos = itemCount
            items.add(LoaderItem{ onRetry.invoke() })
            notifyItemInserted(lastPos)
        }
    }
    fun removeLoading() {
        Log.i("PokemonAdapter", "remove loading")
        items.indexOfFirst { it is LoaderItem }.takeIf { it != -1 }?.let {
            items.removeAt(it)
            notifyItemRemoved(it)
        }
    }
    fun hasLoading(): Boolean{
        return items.any{ it is LoaderItem }
    }
    fun showRetry(){
        Log.i("PokemonAdapter", "show retry")
        items.indexOfFirst { it is LoaderItem }.takeIf { it != -1 }?.let {
            (items[it] as? LoaderItem)?.retry = true
            notifyItemChanged(it)
        }
    }
}

sealed class ViewItemType(val type: Int) {
    object POKEMON : ViewItemType(1)
    object LOADINGVIEW : ViewItemType(2)
}
abstract class AdapterItem(val type: ViewItemType)
class PokemonItem(val pokemon: Pokemon): AdapterItem(ViewItemType.POKEMON)
class LoaderItem(var retry: Boolean = false, val onRetry: () -> Unit): AdapterItem(ViewItemType.LOADINGVIEW)
open class ItemViewHolder(view: View): RecyclerView.ViewHolder(view)
class PokemonItemViewHolder(view: View):  ItemViewHolder(view) {
    private val ui = ItemBinding.bind(view)

    fun show(pokemonItem: PokemonItem) {
        ui.image.load(pokemonItem.pokemon.detail?.image) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_foreground)
        }
        ui.name.text = pokemonItem.pokemon.name
    }
}
class LoaderItemViewHolder(view: View):  ItemViewHolder(view) {
    private val ui = LoaderBinding.bind(view)
    fun showLoader(loaderItem: LoaderItem) {
        ui.retryButton.apply {
            visibility = if (loaderItem.retry) VISIBLE else GONE
            setOnClickListener { loaderItem.onRetry.invoke() }
        }
        ui.loading.visibility = if (loaderItem.retry) GONE else VISIBLE
    }
}