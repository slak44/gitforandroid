package slak.gitforandroid

import java.util.ArrayList

class SelectableAdapterModel<out T>(val thing: T) {
  var isSelected = false
    private set

  fun setSelectStatus(newStatus: Boolean) {
    isSelected = newStatus
  }

  companion object {
    fun <T> getSelectedModels(models: ArrayList<SelectableAdapterModel<T>>): ArrayList<Int> {
      val selectedIndices = models.indices.filterTo(ArrayList<Int>()) { models[it].isSelected }
      return selectedIndices
    }

    fun <T> fromArray(things: Array<T>): ArrayList<SelectableAdapterModel<T>> {
      val newModels = things.mapTo(ArrayList<SelectableAdapterModel<T>>()) {
        SelectableAdapterModel(it)
      }
      return newModels
    }
  }

}
