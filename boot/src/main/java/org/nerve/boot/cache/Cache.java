package org.nerve.boot.cache;

public class Cache {
    private Object data;
    private long time;
    private long expire;
    private int hit;
    private String name;

    public Cache(String name){
        this.name = name;
    }

    public Cache(Object data, long expire) {
        this.data = data;
        this.expire = expire;
        this.time = System.currentTimeMillis();
    }

    public boolean isExpire(){
        return this.expire<System.currentTimeMillis();
    }

    public void onHit(){
        this.hit += 1;
    }

    public Cache snapshot(String name){
        Cache clone = new Cache(data==null?null : data.toString(), expire);
        clone.hit = hit;
        clone.time = time;
        clone.name = name;
        return clone;
    }

    @Override
    public String toString() {
        return "Cache{" +
                "data=" + data +
                ", time=" + time +
                ", expire=" + expire +
                ", hit=" + hit +
                '}';
    }

    public Object getData() {
        return data;
    }

    public long getTime() {
        return time;
    }

    public long getExpire() {
        return expire;
    }

    public int getHit() {
        return hit;
    }

    public String getName() {
        return name;
    }

    public Cache setName(String name) {
        this.name = name;
        return this;
    }
}
