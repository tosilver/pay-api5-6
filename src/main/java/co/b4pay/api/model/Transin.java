package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收款账户
 *
 * @author Tosdom
 */
@Entity
@Table(name = "dst_transin")
public class Transin extends BaseEntity {

    /**
     * 实体编号（唯一标识）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pid;

    private String realname;

    private Integer status;

    @Override
    public String toString() {
        return "Transin{" +
                "id=" + id +
                ", pid='" + pid + '\'' +
                ", realname='" + realname + '\'' +
                ", status=" + status +
                '}';
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
