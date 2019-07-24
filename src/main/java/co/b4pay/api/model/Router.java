package co.b4pay.api.model;


import co.b4pay.api.model.base.BaseEntity;
import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 路由信息
 *
 * @author YK
 * @version $Id: Router.java, v 0.1 2018年4月23日 下午22:17:09 YK Exp $
 */
@Entity
@Table(name = "dst_router")
@Proxy(lazy = false)
public class Router extends BaseEntity {
    private static final long serialVersionUID = -3050115515392663096L;

    @Id
    private String id;                                        // 唯一标识ID

    private String version;                                   // 服务版本

    private Integer status;                               // 状态标记（-1：删除； 0：初始草稿； >=1：其他正常状态；）

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "Router{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", status=" + status +
                '}';
    }
}
