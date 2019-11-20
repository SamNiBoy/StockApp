package com.sn.model;

import java.util.ArrayList;
import java.util.List;

public class TableWrapper{
   public String getColumns() {
        return columns;
    }
    public void setColumns(String columns) {
        this.columns = columns;
    }
    public List<Object> getData() {
        return data;
    }
    public void setData(List<Object> data) {
        this.data = data;
    }
    public int getPage_size() {
        return page_size;
    }
    public void setPage_size(int page_size) {
        this.page_size = page_size;
    }
   //columns: "col1,col2...,coln
   public String columns = "";
   public List<Object> data;
   public int page_size = 10;
}
