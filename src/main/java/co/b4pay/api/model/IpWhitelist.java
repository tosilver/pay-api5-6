package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * IP白名单
 *
 * @author YK
 * @version $Id: IpWhitelist.java, v 0.1 2018年4月21日 下午16:05:09 YK Exp $
 */
@Entity
@Table(name = "dst_ip_whitelist")
public class IpWhitelist extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115515232693586L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    private Long id;                                   // 唯一标识ID
    private String merchantId;                           // 商户
    private String ip;                                   // IP
    private String desc;                                 // 描述

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
