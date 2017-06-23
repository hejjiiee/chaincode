/*
 * code by hjj,
 * method of use:
 * deploy: "args": ["init"]
 * invoke:"args": ["commit","Alice","123456","hjj","Bob","50"]
 *                ["调用函数"，"用户名","密码","合同C","对方ID"，"抵押金"]
 * invoke:"args": ["open","Alice","123456","hjj"]
 *                ["调用函数"，"用户名","密码","合同C"]
 * invoke:"args": ["fuse","Alice","123456","hjj"]
 * 				  ["调用函数"，"用户名","密码","合同C"]
 * query:"args": ["","Alice"]
 *               ["","key值"]
*/
package FSCS;

import java.util.Arrays;

import org.hyperledger.java.shim.ChaincodeBase;
import org.hyperledger.java.shim.ChaincodeStub;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

public class fscs extends ChaincodeBase {

	private Pairing pairing;
	private Field G ;//群G
	private Field GT;//群GT
	private Field Zr;//Zq
	private String m; //要签署的合同
	private Element P,pka,ska,pkeya,skeya,H;
	private Element pkb,skb,pkeyb,skeyb;
	private Element sa,sb,saya,sam;
	private Element ap,sbya,sbm,sbyb;
	private Element BVESa,BVESb;
	account A,B,user;
	int deposit_a,deposit_b;
	boolean coma=false,comb=false;
	@Override
	public String run(ChaincodeStub stub, String function, String[] args) {
		//System.out.println("In run, function:"+function);
		System.out.println(("Greetings from run(): function -> " + function + " | args -> " + Arrays.toString(args)));
		String re=null;
		switch (function) {
		case "init":
			 init(stub, args);
			 break;
		//case "login":
		//	login(stub, args);
		case "commit":
			 re = commit(stub, args);
			return re;
		case "open":
			open(stub, args);
			break;
		case "fuse":
			fuse(stub, args);
			break;
		default:
			System.out.println("function "+function+"not found!!");
		}
		return re;
	
	}
	@Override
	public String query(ChaincodeStub stub, String function, String[] args) {
		String value=stub.getState(args[0]);
		if(value!=null&&!value.isEmpty())
		return "key="+args[0]+"  value="+value;
		else
			return "no value for  "+args[0];	
	}
	@Override
	public String getChaincodeID() {
		String id="fscs";
		System.out.println("The ChaincodeID is "+id);
		return id;
	}
	public String init(ChaincodeStub stub, String[] args)
	 {
		//pairing= PairingFactory.getPairing("a.properties");//从文件a.properties中读取参数初始化双线性群
		 int rBit = 159;
		int qBit = 107;
		TypeACurveGenerator pg = new TypeACurveGenerator(rBit, qBit);
		PairingParameters typeAParams = pg.generate();
		Pairing pairing = PairingFactory.getPairing(typeAParams);
		G = pairing.getG1();//群G
		 GT = pairing.getGT();//群GT
		 Zr = pairing.getZr();//Zq
		 P = G.newRandomElement().getImmutable();//获取生成元P
		 //BigInteger q = Zr.getOrder(); //q是G,Zq的阶
		 System.out.println("生成元p="+P);
		 stub.putState("p", P.toString());//保存生成元P到账本
		 ska = Zr.newRandomElement().getImmutable();
		 pka = P.mulZn(ska);
		 skeya=Zr.newRandomElement().getImmutable();
		 pkeya=P.mulZn(skeya);                          //生成Alice密钥对
		 skb = Zr.newRandomElement().getImmutable();
		 pkb = P.mulZn(skb);
		 skeyb=Zr.newRandomElement().getImmutable();	//生成Bob密钥对
		 pkeyb=P.mulZn(skeyb);
		 A=new account("Alice", "123456", 100, pka, ska, pkeya, skeya);//asset设置为100
		 B=new account("Bob", "123456", 100, pkb, skb, pkeyb, skeyb);//生成Alice和Bob两个账户
		// stub.putState("pkeya", A.pkey.toString());
		// stub.putState("pkeyb", B.pkey.toString());//A,B临时公钥
		 stub.putState(A.name, ""+A.asset);
		 stub.putState(B.name, ""+B.asset);
		 /*if(PreSignAgree())////产生秘密因子s
		 {
			System.out.println("ChainCode初始化完成！！");
		 }*/
		 System.out.println("ChainCode初始化完成！！");
		 return "ChainCode初始化完成！！";
	 }
	public String commit(ChaincodeStub stub, String[] args) {
		if(args.length!=5){
			System.out.println("Incorrect number of arguments:"+args.length);
			return "{\"Error\":\"Incorrect number of arguments. Expecting 5: from, to, amount\"}";
		}
		if(args[0].equals(A.name)&&args[1].equals(A.password))//Alice处理（&&逻辑与 ||逻辑或）
		{
				System.out.println("当前用户: "+A.name);
				if(m!=args[2]&&m!=null){
					System.out.println("contract error!!!"+args.length);
					return "{\"Error\":\"contract error!!!\"}";
				}
				m=args[2];//要签署的合同
				//user=A;//当前用户为A	
				//Alice产生秘密标签
				 Element a;
				 Element t1, t2;
				 a=Zr.newRandomElement().getImmutable();//获取随机数a
				 byte[] source = m.getBytes();
				 H=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
				 System.out.println("消息hash为："+H);
				 t1=pairing.pairing(pkeyb, pkeya);
				 t2=t1.duplicate().powZn(a);
				 byte[] source1 = t2.toBytes();
				 sa=Zr.newElementFromHash(source1, 0, source1.length);//A的秘密因子sa
				 System.out.println("Alice秘密因子："+sa);
				 ap=P.mulZn(a);//aP
				 saya=A.pk.mulZn(sa);//saya
				 sam=H.mulZn(sa);
				
				stub.putState("pkeyb", B.pkey.toString());
				stub.putState("saya", saya.toString());
				stub.putState("sam", sam.toString());  //参数存入区块链账本
				deposit_a = Integer.parseInt(args[4]);
				A.asset=A.asset-deposit_a; //抵押金
				stub.putState(A.name,""+A.asset);
				System.out.println("收取账户"+deposit_a+"押金！");
				 System.out.println("秘密因子成功建立！！，请等待对方操作！！");
				 coma=true;
				 return "{\"Successful\":\"please waiting for B!!!\"}";
		}
		else if(args[0].equals(B.name)&&args[1].equals(B.password)) //Bob处理
		{
			System.out.println("当前用户: "+B.name);
			//user=B;//当前用户为B
			if(!m.equals(args[2])&&m!=null){
				System.out.println("contract error!!!"+args.length);
				return "{\"Error\":\"contract error!!!\"}";
			}
			//m=args[2];//要签署的合同
			byte[] source = args[2].getBytes();
			 H=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
			 System.out.println("合同hash值为："+H);
			//Bob产生秘密标签
			 Element t3, t4;
			 t3=pairing.pairing(pkeya, ap);
			 t4=t3.duplicate().powZn(skeyb);
			 byte[] source2 = t4.toBytes();
			 sb=Zr.newElementFromHash(source2, 0, source2.length);//B的秘密因子sb
			 System.out.println("Bob秘密因子："+sb);
			 sbya=A.pk.mulZn(sb);
			 sbyb=B.pk.mulZn(sb);
			 sbm=H.mulZn(sb);
			 System.out.println("saya:"+saya);
			 System.out.println("sbya:"+sbya);
			 System.out.println("sam:"+sam);
			 System.out.println("sbm:"+sbm);
			 
			 stub.putState("pkeya", A.pkey.toString());
			 stub.putState("sbya", sbya.toString());
			 stub.putState("sbyb", sbyb.toString());
			 stub.putState("sbm", sbm.toString());  //参数存入区块链账本
			//Bob验证参数是否正确
			 if(saya.isEqual(sbya)&&sam.isEqual(sbm))
			 {
				 System.out.println("Bob验证参数成功，秘密因子成功建立！！");
				 deposit_b = Integer.parseInt(args[4]);
					B.asset=B.asset-deposit_b; //抵押金
					stub.putState(B.name,""+B.asset);
					System.out.println("收取账户"+deposit_b+"押金！");
					comb=true;
				 return "{\"Successful\"}";
			 }
			 else {
				 System.out.println("秘密因子建立失败！！");
				 return "{\"Error\":\"please check again!!!\"}";
			 }
		}
		else System.out.println("没有此用户，请重新输入！");
		return "没有此用户，请重新输入！";
	}
	public String open(ChaincodeStub stub, String[] args) {
		if(args.length!=3){
			System.out.println("Incorrect number of arguments:"+args.length);
			return "{\"Error\":\"Incorrect number of arguments. Expecting 4: from, to, amount\"}";
		}
		if(args[0].equals(A.name)&&args[1].equals(A.password))
		{
			System.out.println("当前用户: "+A.name);
			if(comb=true)  //判断Bob是否生成commit交易
			{ 
				String ma=args[2];
				Element BVES=BVESSisn(ma,A.sk,B.pkey,sa);//生成BVES签名
				BVESa=BVES;//测试用
				BVESVer(BVES,saya,B.pkey,sam);//验证BVES签名
				stub.putState("BVESa", BVES.toString());   //Alice的BVES存入账本
				A.asset=A.asset+deposit_a; //退回Alice抵押金
				stub.putState(A.name,""+A.asset);
				System.out.println("BVES成功写入账本！！押金"+deposit_a+"已退回账户！");
				return "BVES成功写入账本！！押金"+deposit_a+"已退回账户！";
			}
			else System.out.println("请等待Bob生成commit！");
			return "请等待Bob生成commit！";
		}
		else if(args[0].equals(B.name)&&args[1].equals(B.password))
		{
			System.out.println("当前用户: "+B.name);
			if(coma=true)  //判断Alice是否生成commit交易
			{
				String ma=args[2];
				Element BVES=BVESSisn(ma,B.sk,A.pkey,sb);//生成BVES签名
				BVESb=BVES;//测试用
				BVESVer(BVES,sbyb,A.pkey,sbm);//验证BVES签名
				stub.putState("BVESb", BVES.toString());     //Bob的BVES存入账本
				B.asset=B.asset+deposit_b; //退回Bob抵押金
				stub.putState(B.name,""+B.asset);
				System.out.println("BVES成功写入账本！！押金"+deposit_b+"已退回账户！");
				return "BVES成功写入账本！！押金"+deposit_b+"已退回账户！";
			}
			else System.out.println("请等待Alice生成commit！");
			return "请等待Alice生成commit！";
		}
	    else System.out.println("没有此用户，请重新输入！");
		return "没有此用户，请重新输入！";
	}
	public String fuse(ChaincodeStub stub, String[] args) {
		if(args.length!=3){
			System.out.println("Incorrect number of arguments:"+args.length);
			return "{\"Error\":\"Incorrect number of arguments. Expecting 2: from, to, amount\"}";
		}
		if(args[0].equals(A.name)&&args[1].equals(A.password))//Alice处理
		{
			System.out.println("当前用户: "+A.name);
			if(true)//判断当前区块和commit区块间隔是否满足条件
			{
				String bves=stub.getState("BVESb");
				if(bves!=null&&!bves.isEmpty())//判断是否出现openb交易
				{
					Element sign;
					//ves=BVESExt(bves,A.skey,sa);
					sign=BVESExt(BVESb,A.skey,sa);//测试用
					System.out.println("成功提取出对方的普通签名： "+sign);
					String contract=args[2];
					byte[] source = contract.getBytes();
					Element hash=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
					if(ver(sign,B.pk,hash))//验证提取出的普通签名
					{
						System.out.println(" 交易成功！！");
						return "成功提取出对方的签名： "+sign+" 交易成功！！";
					}
					else 
						{
						System.out.println(" 交易失败！！");
						return " 交易失败！！";
						}
				}
				else{
					A.asset=A.asset+deposit_b;
					stub.putState(A.name,""+A.asset);
					System.out.println("规定时间内没有出现对方的签名，交易中断，你获得对方"+deposit_b+"比特币作为补偿！");
					System.out.println("当前账户余额："+A.asset+"比特币！");
					return "规定时间内没有出现对方的签名，交易中断，你获得对方"+deposit_b+"比特币作为补偿！";
				}
			}
		}
		else if(args[0].equals(B.name)&&args[1].equals(B.password))//Bob处理
		{
			System.out.println("当前用户: "+B.name);
			if(true)//判断当前区块和commit区块间隔是否满足条件
			{
				String bves=stub.getState("BVESa");
				if(bves!=null&&!bves.isEmpty())//判断是否出现openb交易
				{
					Element sign;
					//ves=BVESExt(bves,B.skey,sb);
					sign=BVESExt(BVESa,B.skey,sb);//测试用
					System.out.println("成功提取出对方的普通签名： "+sign);
					String contract=args[2];
					byte[] source = contract.getBytes();
					Element hash=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
					if(ver(sign,A.pk,hash))//验证提取出的普通签名
					{
						System.out.println("交易成功！！");
						return "成功提取出对方的签名： "+sign+" 交易成功！！";
					}
					else 
					{
					System.out.println(" 交易失败！！");
					return " 交易失败！！";
					}
				}
				else{
					B.asset=B.asset+deposit_a;
					stub.putState(B.name,""+B.asset);
					System.out.println("规定时间内没有出现对方的签名，交易中断，你获得对方"+deposit_a+"比特币作为补偿！");
					System.out.println("当前账户余额："+B.asset+"比特币！");
					return "规定时间内没有出现对方的签名，交易中断，你获得对方"+deposit_a+"比特币作为补偿！";
				}
			}
		}
		else System.out.println("没有此用户，请重新输入！");
		return "没有此用户，请重新输入！";
	}
	public Element BVESSisn(String ma,Element sk,Element pk,Element s)//生成BVES签名
	 {
		 Element t1, t2,h,BVES;
		 byte[] source = ma.getBytes();
		 h=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
		 t1 = Zr.newElement().set(h).add(sk);
		 t2 = t1.duplicate().mulZn(s).invert();//duplicate方法保证t1的值不被改变
	     BVES = pk.mulZn(t2);
	     System.out.println("BVES签名为："+BVES);
		return BVES;
	}
	 public boolean BVESVer(Element BVES,Element sy,Element pk,Element sm)//验证BVES签名
	 {
		 Element t1, t2, t3, t4,t5,t6;
		// t1=G.newElement().set(P).mulZn(sam);
		 t4=G.newElement().set(P).mulZn(sm);
		// t5=pka.mulZn(s);
		 t6=t4.duplicate().add(sy);
		// System.out.println("t1:"+t1);
		// System.out.println("t4:"+t4);
		// System.out.println("t6:"+t6);
		 t2 = pairing.pairing(t6, BVES);
	     t3 = pairing.pairing(P, pk);
	     System.out.println("t2:"+t2);
		 System.out.println("t3:"+t3);
	     if(t2.isEqual(t3))//isEqual
	     {
	    	 System.out.println("验证BVES签名成功！！");
	    	 return true;
	     }
	     else{
	    	 System.out.println("验证BVES签名失败！！");
	    	 return false;
	     }
	 }	 
	 public boolean ver(Element sign,Element pk,Element hash)//验证普通签名
	 {
		 Element t4, t2, t3;
		 t2=pairing.getG1().newElement().set(P).mulZn(hash).add(pk);
		// System.out.println("t5:"+t5);
		// t2 = t5.duplicate().add(ya); 
		// System.out.println("t2:"+t2);
	//	 System.out.println("t5:"+t5);
	     t3 = pairing.pairing(t2,sign);//e(h(m)P+ya,s)
	     t4 = pairing.pairing(P, P);//e(P,P)
	     if(t3.isEqual(t4))
	     {
	    	System.out.println("验证普通签名成功！！");
	    	return true;
	     }
	     else{
	    	 System.out.println("验证普通签名失败！！");
	    	 return false;
	     }   
	}
	public Element BVESExt(Element BVES,Element sk,Element s)//提取出普通签名
	{
		Element t1,t2,t3;
		t1 = sk.invert();
		t2 = t1.duplicate().mulZn(s);
		t3 = BVES.mulZn(t2);
		System.out.println("提取出普通签名为："+t3);
		return t3;
	}

	/* public boolean PreSignAgree()//产生秘密因子
	 {
		 Element a;
		 Element ap,sbya,sbm;
		 Element t1, t2, t3, t4;
		//Alice产生参数
		 a=Zr.newRandomElement().getImmutable();
		 byte[] source = m.getBytes();
		 H=Zr.newElementFromHash(source, 0, source.length);//计算消息的哈希
		 System.out.println("消息hash为："+H);
		 t1=pairing.pairing(pkeyb, pkeya);
		 t2=t1.duplicate().powZn(a);
		 byte[] source1 = t2.toBytes();
		 sa=Zr.newElementFromHash(source1, 0, source1.length);//A的秘密因子sa
		 System.out.println("Alice秘密因子："+sa);
		 ap=P.mulZn(a);//aP
		 saya=pkeya.mulZn(sa);//saya
		 sam=H.mulZn(sa);
		//Bob产生参数
		 t3=pairing.pairing(pkeya, ap);
		 t4=t3.duplicate().powZn(skeyb);
		 byte[] source2 = t4.toBytes();
		 sb=Zr.newElementFromHash(source2, 0, source2.length);//B的秘密因子sb
		 System.out.println("Bob秘密因子："+sb);
		 sbya=pkeya.mulZn(sb);
		 sbm=H.mulZn(sb);
		 System.out.println("saya:"+saya);
		 System.out.println("sbya:"+sbya);
		 System.out.println("sam:"+sam);
		 System.out.println("sbm:"+sbm);
		//Bob验证参数是否正确
		 if(saya.isEqual(sbya)&&sam.isEqual(sbm))
		 {
			 System.out.println("Bob验证参数成功，秘密因子成功建立！！");
			 return true;
		 }
		 else {
			 System.out.println("秘密因子建立失败！！");
			 return false;
		 }
		
	}*/
	/*public String login(ChaincodeStub stub, String[] args) {
		if(args.length!=2){
			System.out.println("Incorrect number of arguments:"+args.length);
			return "{\"Error\":\"Incorrect number of arguments. Expecting 3: from, to, amount\"}";
		}
		if(args[0]==A.name){
			if(args[1]==A.password){
				System.out.println(A.name+" login in:");
				user=A;//当前用户为A
			}
		}
		else if(args[0]==B.name){
			if(args[1]==B.password){
				System.out.println(B.name+" login in:");
				user=B;//当前用户为B
			}
		}
		return null;
	}*/
		
	public static void main(String[] args) throws Exception {
		System.out.println("starting to test chaincode"+args);
		new fscs().start(args);
	}
}
