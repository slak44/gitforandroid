package slak.gitforandroid;

import java.util.ArrayList;

public class SelectableAdapterModel<T> {
  private T thing;
  private boolean selectStatus = false;

  public SelectableAdapterModel(T thing) {
    this.thing = thing;
  }

  public void setSelectStatus(boolean newStatus) {
    selectStatus = newStatus;
  }
  public boolean isSelected() {
    return selectStatus;
  }
  public T getThing() {
    return thing;
  }

  public static <T> ArrayList<Integer> getSelectedModels(ArrayList<SelectableAdapterModel<T>> models) {
    ArrayList<Integer> selectedIndices = new ArrayList<>();
    for (int i = 0; i < models.size(); i++)
      if (models.get(i).selectStatus) selectedIndices.add(i);
    return selectedIndices;
  }

  public static <T> ArrayList<SelectableAdapterModel<T>> fromArray(T[] things) {
    ArrayList<SelectableAdapterModel<T>> newModels = new ArrayList<>();
    for (T thing : things) newModels.add(new SelectableAdapterModel<>(thing));
    return newModels;
  }

}
