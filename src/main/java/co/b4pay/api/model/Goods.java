package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 商品类别信息
 *
 * @author YK
 * @version $Id: Merchant.java, v 0.1 2018年4月20日 下午14:03:09 YK Exp $
 */
@Entity
@Table(name = "dst_goods")
public class Goods extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115515232873085L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    private Integer id;                                     // 唯一标识ID
    private Integer typeId;                                // 类别id
    private String name;                               // 商品名

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
