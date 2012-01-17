package reid;

public class SortUtils {
	
	/**
	 * Modifies data in-place to reverse the elements.
	 * @param data values to be reversed. this list is modified.
	 */
	public static int[] reverse(int[] data) {
		int tmp, n = data.length;
		for(int i = 0; i < n-i-1; i++) {
			tmp = data[i];
			data[i] = data[n-i-1];
			data[n-i-1] = tmp;
		}
		return data;
	}
	
	/**
	 * Sorts (a copy) of the data in-place and returns the indices
	 * of the sorted ordering. so return[0] is the index of the
	 * smallest element in data.
	 * 
	 * @param data array of doubles to sort (not modified in this method)
	 * @return indices in order consistent with an increasing sorted order
	 * 	on data. return[0] is the index of min(data)
	 */
	public static int[] indexSort(double[] data) {
		int[] index = new int[data.length];
		double[] copy = new double[data.length];
		for(int i = 0; i < index.length; i++) {
			index[i] = i;
			copy[i] = data[i];
		}
		quicksort(copy, index);
		return index;
	}
	
	
	private static void quicksort(double[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}

//	 quicksort a[left] to a[right]
	private static void quicksort(double[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

//	 partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

//	 is x < y ?
	private static boolean less(double x, double y) {
	    return (x < y);
	}

//	 exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
	    double swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}

}
