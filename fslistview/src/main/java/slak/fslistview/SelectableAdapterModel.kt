package slak.fslistview

import java.util.ArrayList

internal open class SelectableAdapterModel<out T>(val thing: T) {
  var selected = false
    get set

  companion object {
    fun <T> getSelectedModels(models: ArrayList<SelectableAdapterModel<T>>): ArrayList<Int> =
        models.indices.filterTo(ArrayList<Int>()) { models[it].selected }

    fun <T> fromArray(things: Array<T>): ArrayList<SelectableAdapterModel<T>> =
        things.mapTo(ArrayList<SelectableAdapterModel<T>>()) { SelectableAdapterModel(it) }
  }

}
