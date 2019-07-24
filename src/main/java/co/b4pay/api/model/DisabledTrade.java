package co.b4pay.api.model;

import co.b4pay.api.model.base.BaseEntity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "dst_disabled_trade_info")
@EntityListeners(AuditingEntityListener.class)
public class DisabledTrade extends BaseEntity {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3050115514689954631L;

    /**
     * 实体编号（唯一标识）
     */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;                          // 唯一标识ID
    private String merchantOrderNo;             // 商户订单号
    private Long channelId;                     // 通道ID
    private String channelName;                     // 通道名称
    private Long merchantId;                    // 商户ID
    private String merchantCompany;                    // 商户名称
    private BigDecimal amount;                   //订单金额
    private Date createTime;
    @LastModifiedDate
    private Date closeTime;
    private BigDecimal channelInitAmount;      //关闭订单前的通道余额
    private BigDecimal channelAlterAmount;      //关闭订单后的通道余额
    private Integer status;
    private String text;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchantOrderNo = merchantOrderNo;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantCompany() {
        return merchantCompany;
    }

    public void setMerchantCompany(String merchantCompany) {
        this.merchantCompany = merchantCompany;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }

    public BigDecimal getChannelInitAmount() {
        return channelInitAmount;
    }

    public void setChannelInitAmount(BigDecimal channelInitAmount) {
        this.channelInitAmount = channelInitAmount;
    }

    public BigDecimal getChannelAlterAmount() {
        return channelAlterAmount;
    }

    public void setChannelAlterAmount(BigDecimal channelAlterAmount) {
        this.channelAlterAmount = channelAlterAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
