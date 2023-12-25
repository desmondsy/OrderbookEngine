package Orderbook;

import java.util.*;

public class ListMap<T> {
    private Map<T, Integer> itemToPosition;
    private List<T> items;
    private Random random;

    public ListMap(Random random)
    {
        itemToPosition = new HashMap<>();
        items = new ArrayList<>();
        this.random = random;
    }

    public void addItem(T item)
    {
        items.add(item);
        itemToPosition.put(item, items.size() - 1);
    }

    public void removeItem(T item)
    {
        Integer position = itemToPosition.remove(item);
        T lastItem = items.remove(items.size() - 1);
        if (position != null && !position.equals(items.size())) {
            items.set(position, lastItem);
            itemToPosition.put(lastItem, position);
        }
    }

    public T chooseRandomItem()
    {
        return items.get(random.nextInt(items.size()));
    }

    public boolean isEmpty()
    {
        return items.isEmpty();
    }

    public int size()
    {
        return items.size();
    }

    @Override
    public String toString() {
        return "ListMap{" +
                "itemToPosition=" + itemToPosition +
                ", items=" + items +
                '}';
    }
}
