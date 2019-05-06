package co.b4pay.api.model;


import co.b4pay.api.model.base.BaseEntity;

/**
 * 合作商信息
 * Created by liuwei on 2018/4/27.
 */
public class Partner extends BaseEntity {

    private String company;     //公司名
    private String address;     //地址
    private String contacts;    //联系人
    private String tel;         //联系电话

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }
}
