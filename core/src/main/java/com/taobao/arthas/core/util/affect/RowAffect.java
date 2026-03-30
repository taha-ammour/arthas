package com.taobao.arthas.core.util.affect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 行记录影响反馈
 * Created by vlinux on 15/5/21.
 */
public final class RowAffect extends Affect {

    private final AtomicInteger rowCount = new AtomicInteger();

    public RowAffect() {
    }

    public RowAffect(int initialCount) {
        this.addRowCount(initialCount);
    }

    /**
     * 影响行数统计
     *
     * @param count 行影响计数
     */
    public void addRowCount(int count) {
        rowCount.addAndGet(count);
    }

    /**
     * 获取影响行个数
     *
     * @return 影响行个数
     */
    public int getRowCount() {
        return rowCount.get();
    }

    @Override
    public String toString() {
        return String.format("Affect(row-cnt:%d) cost in %s ms.",
                getRowCount(),
                cost());
    }
}
