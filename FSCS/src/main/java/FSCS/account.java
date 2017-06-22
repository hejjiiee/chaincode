package FSCS;

import it.unisa.dia.gas.jpbc.Element;

public class account {
	public String name;
	public String password;
	public int asset;//账户资产
	public Element pk;
	public Element sk;//长期BVES密钥对
	public Element pkey;
	public Element skey;//临时BVES密钥对
	public account() {
		// TODO Auto-generated constructor stub
	}
	public account(String name,String password,int asset,Element pk,Element sk,Element pkey2,Element skey2) {
		// TODO Auto-generated constructor stub
		this.name=name;
		this.password=password;
		this.asset=asset;
		this.pk=pk;
		this.sk=sk;
		this.pkey=pkey2;
		this.skey=skey2;
	}
}
