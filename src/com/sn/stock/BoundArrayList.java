package com.sn.stock;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class BoundArrayList<T> extends ArrayList<T>{
    private static final long serialVersionUID = 1L;
    static Logger log = Logger.getLogger(BoundArrayList.class);
    int MAX_QUEUE_SIZE = 1000;
    int act_sz = 0;
    public BoundArrayList(int maxsz) {
        super(maxsz);
        MAX_QUEUE_SIZE = maxsz;
    }
    
    public T get(int idx) {
        int act_idx = idx;
        if (act_sz > MAX_QUEUE_SIZE) {
            act_idx += act_sz % MAX_QUEUE_SIZE;
        }
        act_idx = act_idx % MAX_QUEUE_SIZE;
        return super.get(act_idx);
    }
    
    public boolean add(T v) {
        int act_idx = act_sz % MAX_QUEUE_SIZE;
        act_sz++;
        if (act_sz > MAX_QUEUE_SIZE) {
            super.set(act_idx, v);
        }
        else {
            super.add(v);
        }

        return true;
    }
    
    public int size() {
        return super.size();
    }
    
    public void clear() {
    	log.info("now clear BoundArrayList, set act_sz = 0.");
    	act_sz = 0;
    	super.clear();
    }
    
}
