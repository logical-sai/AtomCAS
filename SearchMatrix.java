package com.AtomCAS;
public class SearchMatrix {
    public boolean searchMatrix(int[][] matrix, int target) {
        int low = 0;
        int high = matrix.length;
        int mid = (low+high)/2;
        boolean broken = false;
        while(matrix[mid][0] != target){
            if(matrix[mid][0] < target){
                low = mid;
                mid = (mid + high)/2;
            }else if(matrix[mid][0] > target){
                high = mid;
                mid = (low+mid)/2;
            }
            if(low == high-1){
                broken = true;
                break;
            }else if(low == high) return false;
        }
        if(!broken) return true;
        int mid2 = matrix[low].length/2;
        int low2 = 0;
        int high2 = matrix[low].length;
        while(matrix[low][mid2] != target){
            if(matrix[low][mid2] < target){
                low2 = mid2;
                mid2 = (mid2 + high2)/2;
            }else if(matrix[low][mid2] > target){
                high2 = mid2;
                mid2 = (low2+mid2)/2;
            }
            if(high2 == low2) return false;
        }
        return true;
    }

    static void main(String[] args) {
        SearchMatrix sm = new SearchMatrix();
        int[][] matrix = {{1,3,5,7},{10,11,16,20},{23,30,34,50}};
        System.out.println(sm.searchMatrix(matrix, 1));
    }
}