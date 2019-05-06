package co.b4pay.api.model.base;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页
 *
 * @author YK
 * @version $Id: Pager.java, v 0.1 2010-8-23 下午4:58:42 YK Exp $
 */
public class Page<T> implements Serializable {

    /**
     * 默认页面数据条数
     */
    private static final int PAGE_SIZE = 10;

    /**
     * 符合条件数据总数条数
     */
    private int totalCount;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 当前页码索引
     */
    private int pageIndex = 1;
    /**
     * 当前页第一条
     */
    private int pageFirst;
    /**
     * 每页显示多少条
     */
    private int pageSize = PAGE_SIZE;

    private String orderBy;                               // 排序字段

    private Params params;                                // 查询对象

    /**
     * 条目
     */
    private List<T> list = Collections.emptyList();

    public Page() {
    }

    public Page(List<T> list) {
        this.list = list;
    }

    public Page(int pageIndex, int pageSize, int totalCount) {
        init(pageIndex, pageSize, totalCount);
    }

    public void init(int totalCount) {
        if (totalCount <= 0)
            return;

        this.totalCount = totalCount;
        this.totalPage = totalCount / this.pageSize;
        if (totalCount % this.pageSize > 0) {
            this.totalPage++;
        }
        this.pageFirst = (pageIndex - 1) * this.pageSize;
    }

    public void init(int pageIndex, int pageSize, int totalCount) {
        if (totalCount <= 0)
            return;

        this.totalCount = totalCount;
        this.pageSize = pageSize <= 0 ? PAGE_SIZE : pageSize;
        this.totalPage = totalCount / this.pageSize;
        if (totalCount % this.pageSize > 0) {
            this.totalPage++;
        }
        if (pageIndex < 1) {
            pageIndex = 1;
        }
        if (pageIndex > this.totalPage) {
            pageIndex = this.totalPage;
        }
        this.pageIndex = pageIndex;
        this.pageFirst = (pageIndex - 1) * this.pageSize;
        // if (this.pageFirst > 0)
        // this.pageFirst--;
    }

    public static <T> Page<T> build(int pageIndex, int totalCount) {
        Page<T> page = new Page<>();
        page.init(pageIndex, -1, totalCount);
        return page;
    }

    public static <T> Page<T> build(int pageIndex, int pageSize, int totalCount) {
        Page<T> page = new Page<>();
        page.init(pageIndex, pageSize, totalCount);
        return page;
    }

    /**
     * Getter method for property <tt>totalCount</tt>.
     *
     * @return property value of totalCount
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Setter method for property <tt>totalCount</tt>.
     *
     * @param totalCount value to be assigned to property totalCount
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Getter method for property <tt>totalPage</tt>.
     *
     * @return property value of totalPage
     */
    public int getTotalPage() {
        return totalPage;
    }

    /**
     * Setter method for property <tt>totalPage</tt>.
     *
     * @param totalPage value to be assigned to property totalPage
     */
    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    /**
     * Getter method for property <tt>pageIndex</tt>.
     *
     * @return property value of pageIndex
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * Setter method for property <tt>pageIndex</tt>.
     *
     * @param pageIndex value to be assigned to property pageIndex
     */
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    /**
     * Getter method for property <tt>pageFirst</tt>.
     *
     * @return property value of pageFirst
     */
    public int getPageFirst() {
        return pageFirst;
    }

    /**
     * Setter method for property <tt>pageFirst</tt>.
     *
     * @param pageFirst value to be assigned to property pageFirst
     */
    public void setPageFirst(int pageFirst) {
        this.pageFirst = pageFirst;
    }

    /**
     * Getter method for property <tt>pageSize</tt>.
     *
     * @return property value of pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Setter method for property <tt>pageSize</tt>.
     *
     * @param pageSize value to be assigned to property pageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Getter method for property <tt>list</tt>.
     *
     * @return property value of list
     */
    public List<T> getList() {
        return list;
    }

    /**
     * Setter method for property <tt>list</tt>.
     *
     * @param list value to be assigned to property list
     */
    public void setList(List<T> list) {
        this.list = list;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }
}