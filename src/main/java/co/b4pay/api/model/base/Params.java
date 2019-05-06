package co.b4pay.api.model.base;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.*;

/**
 * 参数对象
 * <p>
 * 注：详细参见各个服务管理对应的参数
 * </p>
 *
 * @author YK
 */
public class Params extends HashMap<String, Object> implements Serializable {
    /**
     * class关键字
     */
    private static final String CLASS_STR = "class";

    /**
     * 空的参数对象
     */
    private static final Params EMPTY_PARAMS = new EmptyParams();

    /**
     *
     */
    private static final long serialVersionUID = 4240671399415517892L;

    public Params() {
    }

    private Params(int initialCapacity) {
        super(initialCapacity);
    }

    private Params(Params params) {
        super(params);
    }

    private <T extends BaseEntity> Params(T entity) {
        if (entity != null) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
            PropertyDescriptor[] descriptor = beanWrapper.getPropertyDescriptors();
            for (int i = 0; i < descriptor.length; i++) {
                String name = descriptor[i].getName();
                if (CLASS_STR.equals(name)) {
                    continue;
                }
                Object value = beanWrapper.getPropertyValue(name);
                if (org.springframework.util.ObjectUtils.isEmpty(value)) {
                    continue;
                }
                this.put(name, value);
            }
        }
    }

    public static Params create() {
        return new Params();
    }

    /**
     * 创建Params实例
     *
     * @param key
     * @param value 初始化容量
     * @return
     */
    public static Params create(String key, Object value) {
        return new Params(key, value);
    }

    public static <T extends BaseEntity> Params create(T entity) {
        return new Params(entity);
    }

    public Params add(String key, Object value) {
        this.put(key, value);
        return this;
    }

    private Params(String key, Object value) {
        this.put(key, value);
    }

    public static <T extends BaseEntity> Params create(T entity, int pageSize) {
        return create(entity, 1, pageSize, -1, null);
    }

    public static <T extends BaseEntity> Params create(Params params, int pageIndex, int pageSize, int totalCount) {
        return create(params, pageIndex, pageSize, totalCount, null);
    }

    public static <T extends BaseEntity> Params create(T entity, int pageIndex, int pageSize, int totalCount) {
        return create(entity, pageIndex, pageSize, totalCount, null);
    }

    public static <T extends BaseEntity> Params create(Params params, int pageIndex, int pageSize, int total, String orderBy) {
        return new Params(params).initPage(pageIndex, pageSize, total, orderBy);
    }

    public static <T extends BaseEntity> Params create(T entity, int pageIndex, int pageSize, int total, String orderBy) {
        return new Params(entity).initPage(pageIndex, pageSize, total, orderBy);
    }

    public static Params empty() {
        return EMPTY_PARAMS;
    }

    public <T extends BaseEntity> Params initPage(Page<T> page) {
        if (this instanceof EmptyParams) {
            return this;
        }
        this.put("pageFirst", page.getPageFirst());
        this.put("pageSize", page.getPageSize());
        this.put("pageIndex", page.getPageIndex()); // 当前页码索引
        this.put("totalCount", page.getTotalCount()); // 当前页总条数
        this.put("totalPage", page.getTotalPage()); // 总页数
        this.put("orderBy", page.getOrderBy());

        return this;
    }

    public <T extends BaseEntity> Params initPage(int pageIndex, int pageSize, int totalCount, String orderBy) {
        // if (totalCount <= 1) {// 不分页，只查询指定条数
        // // this.put("pageFirst", 0);
        // // this.put("pageSize", pageSize);
        // this.put("orderBy", orderBy);
        // return this;
        // }

        Page<T> page = Page.build(pageIndex < 1 ? 1 : pageIndex, pageSize, totalCount);
        this.put("pageFirst", page.getPageFirst());
        this.put("pageSize", page.getPageSize());
        this.put("pageIndex", page.getPageIndex()); // 当前页码索引
        this.put("totalCount", page.getTotalCount()); // 当前页总条数
        this.put("totalPage", page.getTotalPage()); // 总页数
        this.put("orderBy", orderBy);

        return this;
    }

    public Params limit(int limit) {
        this.put("pageFirst", 0);
        this.put("pageSize", limit);
        this.put("pageIndex", 1);
        return this;
    }

    public Params limit(int offset, int limit) {
        if (this instanceof EmptyParams) {
            return this;
        }
        this.put("pageFirst", offset);
        this.put("pageSize", limit);
        this.put("pageIndex", 1);
        return this;
    }

    public Params orderBy(String orderBy) {
        if (this instanceof EmptyParams) {
            return this;
        }
        if (orderBy == null || orderBy.isEmpty()) {
            return this;
        }
        this.put("orderBy", orderBy);
        return this;
    }

    public String getString(Object key) throws DtoException {
        Object o = super.get(key);
        if (o == null || "".equals(o)) {
            throw new DtoException("Params[\"" + key + "\"] not found.");
        }
        return o.toString();
    }

    public int getInt(Object key) throws DtoException {
        Object o = super.get(key);
        if (o == null || "".equals(o)) {
            throw new DtoException("Params[\"" + key + "\"] not found.");
        }
        return Integer.parseInt(o.toString());
    }

    public long getLong(Object key) throws DtoException {
        Object o = super.get(key);
        if (o == null || "".equals(o)) {
            throw new DtoException("Params[\"" + key + "\"] not found.");
        }
        return Long.parseLong(o.toString());
    }

    /**
     * @serial include
     */
    private static class EmptyParams extends Params implements Serializable {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 7395488185614369607L;

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public Set<String> keySet() {
            return Collections.emptySet();
        }

        public Collection<Object> values() {
            return Collections.emptySet();
        }

        public Set<Entry<String, Object>> entrySet() {
            return Collections.emptySet();
        }

        public boolean equals(Object o) {
            return (o instanceof Map) && ((Map<?, ?>) o).isEmpty();
        }

        public int hashCode() {
            return 0;
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Params add(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_PARAMS;
        }
    }

}
