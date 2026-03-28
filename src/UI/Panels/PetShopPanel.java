package UI.Panels;

import Core.Game;
import Pets.*;
import Player.Player;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class PetShopPanel extends JPanel {

    private final Player player;
    private final PetInventory inventory;
    private final Runnable onBack;

    private Font titleFont, bodyFont, smallFont, tinyFont;
    private Image shopBackground;
    private Image[] fgEggImages = new Image[5];

    private static final int PANEL_W       = (int)(Game.getRealScreenWidth()  * 0.88);
    private static final int PANEL_H       = (int)(Game.getRealScreenHeight() * 0.84);
    private static final int SELLER_W      = Game.scale(115);
    private static final int HEADER_H      = Game.scale(52);
    private static final int FOOTER_H      = Game.scale(36);
    private static final int CONTENT_X_OFF = SELLER_W + Game.scale(14);

    private int activeTab = 0;
    private static final String[] TABS = {"SHOP", "INDEX"};

    private int   selectedEggIdx  = 0;
    private float displayedEggIdx = 0f;
    private static final float LERP = 0.16f;

    private boolean hoverLeft = false, hoverRight = false, hoverBuy = false;

    private static final int FG_EGG_W = Game.scale(200), FG_EGG_H = Game.scale(200);
    private static final int BG_EGG_W = Game.scale(110), BG_EGG_H = Game.scale(110);
    private static final int ARR_W    = Game.scale(48),  ARR_H    = Game.scale(48);

    private static final Color[] EGG_COL = {
            new Color(160,160,160), new Color(80,200,80),
            new Color(80,120,255),  new Color(180,60,255), new Color(255,180,0)
    };
    private static final Color[] EGG_BDR = {
            new Color(200,200,200), new Color(110,230,110),
            new Color(130,165,255), new Color(210,90,255), new Color(255,215,55)
    };

    private boolean isOpening = false;
    private long openStart = 0;
    private static final long OPEN_DUR = 3200;
    private Pet.PetType openResult = null;
    private List<Pet.PetType> spinPool = new ArrayList<>();
    private int  spinIdx = 0;
    private long lastSpin = 0, spinMs = 55;
    private boolean showResult = false;
    private long resultTime = 0;
    private static final long RESULT_LINGER = 2800;

    private int selectedForInfo = -1;
    private int indexScrollY = 0, indexMaxScroll = 0;
    private int dragStartY = -1, dragStartScroll = 0;

    private static final int IDX_COLS   = 4;
    private static final int IDX_CARD_W = Game.scale(148), IDX_CARD_H = Game.scale(112);
    private static final int IDX_GAP_X  = Game.scale(8),   IDX_GAP_Y  = Game.scale(10);
    private static final int DETAIL_H   = Game.scale(100);

    private String statusMsg = ""; private long statusTime = 0;
    private static final long STATUS_MS = 3500;

    private Timer repaintTimer;

    public PetShopPanel(Player player, PetInventory inventory, Runnable onBack) {
        this.player = player; this.inventory = inventory; this.onBack = onBack;
        setLayout(null); setOpaque(false);
        setBounds(0, 0, (int)Game.getRealScreenWidth(), (int)Game.getRealScreenHeight());
        loadResources();
        setupInput();
        repaintTimer = new Timer(30, e -> {
            displayedEggIdx += (selectedEggIdx - displayedEggIdx) * LERP;
            repaint();
        });
        repaintTimer.setRepeats(true);
    }

    @Override public void setVisible(boolean v) {
        super.setVisible(v);
        if (v) repaintTimer.start(); else repaintTimer.stop();
    }

    private void loadResources() {
        try {
            titleFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fonts/PixelPurl.ttf")).deriveFont((float)Game.scale(20));
            bodyFont  = titleFont.deriveFont((float)Game.scale(13));
            smallFont = titleFont.deriveFont((float)Game.scale(11));
            tinyFont  = titleFont.deriveFont((float)Game.scale(9));
        } catch (Exception e) {
            titleFont = new Font("Arial", Font.BOLD,  Game.scale(20));
            bodyFont  = new Font("Arial", Font.PLAIN, Game.scale(13));
            smallFont = new Font("Arial", Font.PLAIN, Game.scale(11));
            tinyFont  = new Font("Arial", Font.PLAIN, Game.scale(9));
        }
        loadImg(s -> shopBackground = s, "/WATVA/Other/ShopBackground.png");
        for (int i = 0; i < 5; i++) {
            final int fi = i;
            loadImg(s -> fgEggImages[fi] = s, "/WATVA/Other/Egg" + (i+1) + ".png");
        }
    }

    @FunctionalInterface interface ImgConsumer { void set(Image img); }
    private void loadImg(ImgConsumer c, String path) {
        try { c.set(ImageIO.read(getClass().getResourceAsStream(path))); } catch (Exception ignored) {}
    }

    private int panelX()   { return (getWidth()  - PANEL_W) / 2; }
    private int panelY()   { return (getHeight() - PANEL_H) / 2; }
    private int contentX() { return panelX() + CONTENT_X_OFF; }
    private int contentY() { return panelY() + HEADER_H; }
    private int contentW() { return PANEL_W  - CONTENT_X_OFF - Game.scale(8); }
    private int contentH() { return PANEL_H  - HEADER_H - FOOTER_H; }
    private int shopCX()   { return contentX() + contentW() / 2; }
    private int shopCY()   { return contentY() + (contentH() - DETAIL_H_SHOP()) / 2 - Game.scale(10); }
    private int DETAIL_H_SHOP() { return Game.scale(100); }

    private Rectangle fgEggBounds()    { return new Rectangle(shopCX()-FG_EGG_W/2, shopCY()-FG_EGG_H/2, FG_EGG_W, FG_EGG_H); }
    private Rectangle arrowLBounds()   { Rectangle fg=fgEggBounds(); return new Rectangle(fg.x-ARR_W-Game.scale(10), fg.y+fg.height/2-ARR_H/2, ARR_W, ARR_H); }
    private Rectangle arrowRBounds()   { Rectangle fg=fgEggBounds(); return new Rectangle(fg.x+fg.width+Game.scale(10), fg.y+fg.height/2-ARR_H/2, ARR_W, ARR_H); }
    private Rectangle buyBounds()      { Rectangle fg=fgEggBounds(); int bw=Game.scale(158),bh=Game.scale(38); return new Rectangle(fg.x+(fg.width-bw)/2, fg.y+fg.height+Game.scale(128), bw, bh); }
    private Rectangle backBounds()     { return new Rectangle(panelX()+Game.scale(8), panelY()+Game.scale(8), Game.scale(90), Game.scale(36)); }
    private Rectangle tabBounds(int i) { int tw=Game.scale(140); return new Rectangle(panelX()+PANEL_W-(TABS.length-i)*(tw+Game.scale(6)), panelY()+Game.scale(8), tw, Game.scale(36)); }

    private Rectangle idxCardBounds(int gi) {
        int gapX = (contentW() - IDX_COLS*IDX_CARD_W) / (IDX_COLS+1);
        int col=gi%IDX_COLS, row=gi/IDX_COLS;
        return new Rectangle(contentX()+gapX+col*(IDX_CARD_W+gapX),
                contentY()+Game.scale(6)+row*(IDX_CARD_H+IDX_GAP_Y)-indexScrollY,
                IDX_CARD_W, IDX_CARD_H);
    }
    private Rectangle upgradeBounds() {
        return new Rectangle(contentX()+contentW()-Game.scale(152), contentY()+contentH()-DETAIL_H+Game.scale(56), Game.scale(148), Game.scale(32));
    }
    private Rectangle selectBounds() {
        return new Rectangle(contentX()+contentW()-Game.scale(152), contentY()+contentH()-DETAIL_H+Game.scale(94), Game.scale(148), Game.scale(32));
    }

    private void setupInput() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { updateHover(e.getPoint()); }
            @Override public void mouseClicked(MouseEvent e) { doClick(e.getPoint()); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (activeTab==1) { indexScrollY=Math.max(0,Math.min(indexMaxScroll, indexScrollY+e.getUnitsToScroll()*Game.scale(18))); repaint(); }
            }
            @Override public void mousePressed(MouseEvent e)  { if (activeTab==1) { dragStartY=e.getY(); dragStartScroll=indexScrollY; } }
            @Override public void mouseDragged(MouseEvent e)  {
                if (activeTab==1&&dragStartY>=0) { indexScrollY=Math.max(0,Math.min(indexMaxScroll,dragStartScroll+(dragStartY-e.getY()))); repaint(); }
            }
            @Override public void mouseReleased(MouseEvent e) { dragStartY=-1; }
        };
        addMouseListener(ma); addMouseMotionListener(ma); addMouseWheelListener(ma);
    }

    private void updateHover(Point p) {
        hoverLeft  = activeTab==0 && arrowLBounds().contains(p);
        hoverRight = activeTab==0 && arrowRBounds().contains(p);
        hoverBuy   = activeTab==0 && buyBounds().contains(p);
    }

    private void doClick(Point p) {
        if (backBounds().contains(p)) { onBack.run(); return; }
        for (int i=0;i<TABS.length;i++) {
            if (tabBounds(i).contains(p)) { activeTab=i; if(i==1){indexScrollY=0;selectedForInfo=-1;} repaint(); return; }
        }
        if (isOpening) return;
        if (showResult) { showResult=false; return; }

        if (activeTab==0) {
            if (arrowLBounds().contains(p) && selectedEggIdx>0) { selectedEggIdx--; return; }
            if (arrowRBounds().contains(p) && selectedEggIdx<4) { selectedEggIdx++; return; }
            if (buyBounds().contains(p))                         { tryBuy(); return; }
        } else {
            Pet.PetType[] types=Pet.PetType.values();
            int avail = contentH()-DETAIL_H-Game.scale(4);
            for (int i=0;i<types.length;i++) {
                Rectangle cb=idxCardBounds(i);
                if (cb.contains(p)&&p.y>contentY()&&p.y<contentY()+avail) {
                    selectedForInfo=(selectedForInfo==i)?-1:i; repaint(); return;
                }
            }
            if (selectedForInfo>=0 && upgradeBounds().contains(p)) { tryUpgrade(types[selectedForInfo]); return; }
            if (selectedForInfo>=0 && selectBounds().contains(p))  { toggleSelect(types[selectedForInfo]); return; }
        }
    }

    private void tryBuy() {
        Egg.EggType egg=Egg.EggType.values()[selectedEggIdx];
        if (player.getCoins()<egg.cost) { setStatus("Need "+egg.cost+" coins! (have "+player.getCoins()+")"); return; }
        player.setCoins(player.getCoins()-egg.cost);
        openResult=Egg.openEgg(egg,inventory);
        inventory.save();
        startAnim();
    }

    private void startAnim() {
        isOpening=true; showResult=false; openStart=System.currentTimeMillis();
        spinMs=55; lastSpin=openStart; spinIdx=0; spinPool.clear();
        Random rng=new Random(); Pet.PetType[] all=Pet.PetType.values();
        for (int i=0;i<13;i++) spinPool.add(all[rng.nextInt(all.length)]);
        spinPool.add(openResult);
    }

    private void tickAnim() {
        long el=(System.currentTimeMillis()-openStart);
        float prog=Math.min(1f,(float)el/OPEN_DUR);
        spinMs=(long)(55+600*Math.pow(prog,2.5));
        long now=System.currentTimeMillis();
        if (now-lastSpin>=spinMs && spinIdx<spinPool.size()-1) { spinIdx++; lastSpin=now; }
        if (el>=OPEN_DUR) {
            isOpening=false; showResult=true; resultTime=System.currentTimeMillis();
            spinIdx=spinPool.size()-1; activeTab=1; indexScrollY=0;
            setStatus("You got: "+openResult.displayName+"  ["+openResult.rarity.name+"]!");
        }
    }

    private void tryUpgrade(Pet.PetType type) {
        if (!inventory.isUnlocked(type)) { setStatus("You don't own this pet!"); return; }
        Pet pet=inventory.getPet(type);
        if (pet==null||!pet.canUpgrade()) { setStatus(type.displayName+" is MAX level!"); return; }
        int cc=pet.getUpgradeCoinCost(), dn=pet.getDupesRequired(), dh=inventory.getDuplicateCount(type);
        if (player.getCoins()<cc) { setStatus("Need "+cc+" coins!"); return; }
        if (dh<dn) { setStatus("Need "+dn+"x "+type.displayName+" (have "+dh+")"); return; }
        player.setCoins(player.getCoins()-cc);
        inventory.upgradePet(type); inventory.save();
        setStatus(type.displayName+" upgraded to Lv "+inventory.getPet(type).getLevel()+"!");
    }

    private void toggleSelect(Pet.PetType type) {
        if (!inventory.isUnlocked(type)) { setStatus("Unlock "+type.displayName+" first!"); return; }
        if (inventory.getSelectedPetType()==type) { inventory.deselectPet(); setStatus("Pet deselected."); }
        else { inventory.selectPet(type); setStatus(type.displayName+" selected for next run!"); }
        inventory.save();
    }

    private void setStatus(String m) { statusMsg=m; statusTime=System.currentTimeMillis(); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (isOpening) tickAnim();

        g2.setColor(new Color(0,0,0,170)); g2.fillRect(0,0,getWidth(),getHeight());
        int px=panelX(),py=panelY();

        if (shopBackground!=null) g2.drawImage(shopBackground,px,py,PANEL_W,PANEL_H,null);
        else { g2.setColor(new Color(18,12,28)); g2.fillRoundRect(px,py,PANEL_W,PANEL_H,Game.scale(18),Game.scale(18)); }
        g2.setColor(new Color(90,60,25)); g2.setStroke(new BasicStroke(Game.scale(3)));
        g2.drawRoundRect(px,py,PANEL_W,PANEL_H,Game.scale(18),Game.scale(18));

        drawHeader(g2); drawContent(g2); drawFooter(g2);
        if (isOpening)                                                     drawSpinAnim(g2);
        if (showResult && System.currentTimeMillis()-resultTime<RESULT_LINGER) drawResult(g2);
    }

    private void drawHeader(Graphics2D g2) {
        int px=panelX(),py=panelY();
        g2.setColor(new Color(0,0,0,60)); g2.fillRect(px,py,PANEL_W,HEADER_H);
        g2.setColor(new Color(90,60,25,140)); g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawLine(px+Game.scale(6),py+HEADER_H-1,px+PANEL_W-Game.scale(6),py+HEADER_H-1);

        Rectangle bb=backBounds();
        g2.setColor(new Color(160,40,40)); g2.fillRoundRect(bb.x,bb.y,bb.width,bb.height,Game.scale(8),Game.scale(8));
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawRoundRect(bb.x,bb.y,bb.width,bb.height,Game.scale(8),Game.scale(8));
        g2.setFont(bodyFont.deriveFont(Font.BOLD)); drawC(g2,"<- BACK",bb);

        g2.setFont(titleFont.deriveFont(Font.BOLD,(float)Game.scale(22)));
        g2.setColor(new Color(255,210,60));
        String ttl="PET SHOPPE";
        drawOutlined(g2, ttl, px+CONTENT_X_OFF+(PANEL_W-CONTENT_X_OFF-Game.scale(8)-fw(g2,ttl))/2, py+HEADER_H-Game.scale(10));

        for (int i=0;i<TABS.length;i++) {
            Rectangle r=tabBounds(i); boolean act=(i==activeTab);
            g2.setColor(act?new Color(55,38,10):new Color(28,18,5));
            g2.fillRoundRect(r.x,r.y,r.width,r.height,Game.scale(8),Game.scale(8));
            g2.setColor(act?new Color(210,150,30):new Color(100,70,20));
            g2.setStroke(new BasicStroke(act?Game.scale(2):Game.scale(1)));
            g2.drawRoundRect(r.x,r.y,r.width,r.height,Game.scale(8),Game.scale(8));
            g2.setFont(smallFont.deriveFont(act?Font.BOLD:Font.PLAIN));
            g2.setColor(act?Color.WHITE:new Color(180,150,100));
            drawC(g2,TABS[i],r);
        }
    }

    private void drawContent(Graphics2D g2) {
        if (activeTab==0) drawCarousel(g2); else drawIndex(g2);
    }

    private void drawCarousel(Graphics2D g2) {
        Egg.EggType[] eggs=Egg.EggType.values();
        int cx=shopCX(), cy=shopCY();

        for (int off=-2;off<=2;off++) {
            if (off==0) continue;
            float vpos=off+(selectedEggIdx-displayedEggIdx);
            int di=selectedEggIdx+off;
            if (di<0||di>=eggs.length) continue;
            float dist=Math.abs(vpos);
            float alpha=Math.min(1f, Math.max(0f, 1f-(dist-0.5f)*0.75f));
            float scale=Math.max(0.52f,1f-dist*0.24f);
            if (alpha<=0.02f) continue;
            int bw=(int)(BG_EGG_W*scale),bh=(int)(BG_EGG_H*scale);
            int spread=(int)(FG_EGG_W/2+Game.scale(16)+Math.abs(off)*Game.scale(72));
            int bx=cx+(off<0?-spread-bw/2:spread-bw/2);
            int by=cy-bh/2+Game.scale(28);
            Composite oc=g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            if (fgEggImages[di]!=null) g2.drawImage(fgEggImages[di],bx,by,bw,bh,null);
            else fallbackEgg(g2,bx+bw/2,by+bh/2,bw/2,bh/2,EGG_COL[di]);
            g2.setComposite(oc);
        }

        Rectangle fg=fgEggBounds();
        Egg.EggType cur=eggs[selectedEggIdx];
        Color gc=EGG_BDR[selectedEggIdx];
        g2.setColor(new Color(gc.getRed(),gc.getGreen(),gc.getBlue(),35));
        g2.fillOval(fg.x-Game.scale(20),fg.y-Game.scale(20),fg.width+Game.scale(40),fg.height+Game.scale(40));
        if (fgEggImages[selectedEggIdx]!=null) g2.drawImage(fgEggImages[selectedEggIdx],fg.x,fg.y,fg.width,fg.height,null);
        else fallbackEgg(g2,cx,cy,FG_EGG_W/2-Game.scale(8),FG_EGG_H/2-Game.scale(8),EGG_COL[selectedEggIdx]);

        int infoX=contentX()+Game.scale(10), infoW=contentW()-Game.scale(20);
        int infoY=fg.y+fg.height+Game.scale(4), infoH=Game.scale(92);
        g2.setColor(new Color(0,0,0,130)); g2.fillRoundRect(infoX,infoY,infoW,infoH,Game.scale(10),Game.scale(10));
        g2.setColor(EGG_BDR[selectedEggIdx]); g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawRoundRect(infoX,infoY,infoW,infoH,Game.scale(10),Game.scale(10));

        g2.setFont(titleFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE);
        drawOutlined(g2, cur.displayName,infoX+Game.scale(12),infoY+Game.scale(22));
        g2.setFont(bodyFont.deriveFont(Font.BOLD)); g2.setColor(new Color(255,215,50));
        String pr=cur.cost+" coins"; drawOutlined(g2, pr,infoX+infoW-fw(g2,pr)-Game.scale(12),infoY+Game.scale(22));
        g2.setFont(smallFont); g2.setColor(new Color(190,190,190));
        drawOutlined(g2, cur.description,infoX+Game.scale(12),infoY+Game.scale(40));
        drawRarityBars(g2,infoX+Game.scale(12),infoY+Game.scale(54),infoW-Game.scale(24),cur.rarityWeights);

        Rectangle buy=buyBounds(); boolean ca=player.getCoins()>=cur.cost;
        g2.setColor(ca?(hoverBuy?new Color(50,160,50):new Color(40,120,40)):new Color(90,35,35));
        g2.fillRoundRect(buy.x,buy.y,buy.width,buy.height,Game.scale(10),Game.scale(10));
        g2.setColor(ca?new Color(80,220,80):new Color(180,80,80)); g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawRoundRect(buy.x,buy.y,buy.width,buy.height,Game.scale(10),Game.scale(10));
        g2.setFont(bodyFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE); drawC(g2,"BUY EGG",buy);

        drawArrow(g2,arrowLBounds(),false,selectedEggIdx>0);
        drawArrow(g2,arrowRBounds(),true, selectedEggIdx<4);

        int dR=Game.scale(5),dG=Game.scale(14);
        int dsX=cx-(eggs.length*dG)/2, dsY=buy.y+buy.height+Game.scale(12);
        for (int i=0;i<eggs.length;i++) {
            g2.setColor(i==selectedEggIdx?EGG_BDR[i]:new Color(75,75,75));
            g2.fillOval(dsX+i*dG-dR,dsY-dR,dR*2,dR*2);
        }
    }

    private void drawArrow(Graphics2D g2, Rectangle r, boolean right, boolean en) {
        Color bg=en?(right?hoverRight:hoverLeft)?new Color(80,60,20):new Color(50,38,12):new Color(25,20,10);
        Color fc=en?new Color(220,170,50):new Color(70,60,40);
        g2.setColor(bg); g2.fillRoundRect(r.x,r.y,r.width,r.height,Game.scale(10),Game.scale(10));
        g2.setColor(fc); g2.setStroke(new BasicStroke(Game.scale(2)));
        g2.drawRoundRect(r.x,r.y,r.width,r.height,Game.scale(10),Game.scale(10));
        g2.setColor(fc); g2.setFont(titleFont.deriveFont(Font.BOLD,(float)Game.scale(20)));
        drawC(g2,right?"->":"<-",r);
    }

    private void fallbackEgg(Graphics2D g2, int cx, int cy, int rw, int rh, Color c) {
        g2.setColor(c.darker()); g2.fillOval(cx-rw,cy-rh,rw*2,(int)(rh*2.2));
        g2.setColor(c);          g2.fillOval(cx-rw+Game.scale(3),cy-rh+Game.scale(3),rw*2-Game.scale(6),(int)(rh*2.2-Game.scale(6)));
        g2.setColor(new Color(255,255,255,65)); g2.fillOval(cx-rw/2,cy-(int)(rh*0.6),rw/2,rh/3);
    }

    private void drawRarityBars(Graphics2D g2, int x, int y, int tw, double[] w) {
        Pet.Rarity[] r=Pet.Rarity.values();
        int bh=Game.scale(6),sp=Game.scale(3);
        int bw=(tw-sp*(r.length-1))/r.length;
        for (int i=0;i<r.length;i++) {
            int bx=x+i*(bw+sp);
            g2.setColor(new Color(40,40,40)); g2.fillRoundRect(bx,y,bw,bh,Game.scale(3),Game.scale(3));
            int fl=(int)(bw*w[i]);
            if (fl>0) { Color c=r[i].color; g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),200)); g2.fillRoundRect(bx,y,fl,bh,Game.scale(3),Game.scale(3)); }
        }
        g2.setFont(tinyFont);
        for (int i=0;i<r.length;i++) {
            if (w[i]>0) {
                int bx=x+i*(bw+sp); g2.setColor(r[i].color);
                String pct=(int)(w[i]*100)+"%"; drawOutlined(g2, pct,bx+(bw-fw(g2,pct))/2,y+bh+Game.scale(12));
            }
        }
    }

    private void drawIndex(Graphics2D g2) {
        Pet.PetType[] types=Pet.PetType.values();
        int rows=(int)Math.ceil((double)types.length/IDX_COLS);
        int totalH=rows*(IDX_CARD_H+IDX_GAP_Y)+Game.scale(12);
        int avail=contentH()-DETAIL_H-Game.scale(10);
        indexMaxScroll=Math.max(0,totalH-avail);

        Shape oc=g2.getClip();
        g2.setClip(contentX(),contentY(),contentW(),avail);
        for (int i=0;i<types.length;i++) {
            Rectangle r=idxCardBounds(i);
            if (r.y+r.height<contentY()||r.y>contentY()+avail) continue;
            drawIdxCard(g2,i,types[i]);
        }
        g2.setClip(oc);

        if (indexMaxScroll>0) {
            int sbX=contentX()+contentW()-Game.scale(6);
            int thumbH=Math.max(Game.scale(28),(int)((float)avail/totalH*avail));
            int thumbY=contentY()+(int)((float)indexScrollY/indexMaxScroll*(avail-thumbH));
            g2.setColor(new Color(50,50,50,140)); g2.fillRoundRect(sbX,contentY(),Game.scale(5),avail,Game.scale(3),Game.scale(3));
            g2.setColor(new Color(160,120,40,200)); g2.fillRoundRect(sbX,thumbY,Game.scale(5),thumbH,Game.scale(3),Game.scale(3));
        }

        drawDetailStrip(g2,types);
    }

    private void drawIdxCard(Graphics2D g2, int gi, Pet.PetType type) {
        Rectangle r=idxCardBounds(gi);
        boolean u=inventory.isUnlocked(type), sel=(type==inventory.getSelectedPetType());
        boolean info=(selectedForInfo==gi);
        Color rc=type.rarity.color;
        g2.setColor(u?(sel?new Color(20,50,20):(info?new Color(38,28,50):new Color(22,16,34))):new Color(12,10,18));
        g2.fillRoundRect(r.x,r.y,r.width,r.height,Game.scale(10),Game.scale(10));
        g2.setColor(u?(info?Color.WHITE:rc):rc.darker().darker());
        g2.setStroke(new BasicStroke(info?Game.scale(3):Game.scale(2)));
        g2.drawRoundRect(r.x,r.y,r.width,r.height,Game.scale(10),Game.scale(10));

        int icS=Game.scale(36),icX=r.x+Game.scale(6),icY=r.y+(r.height-icS)/2;
        if (u) {
            g2.setColor(rc.darker()); g2.fillOval(icX,icY,icS,icS);
            g2.setColor(rc); g2.fillOval(icX+Game.scale(2),icY+Game.scale(2),icS-Game.scale(4),icS-Game.scale(4));
            g2.setFont(titleFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE);
            drawC(g2,String.valueOf(type.displayName.charAt(0)),new Rectangle(icX,icY,icS,icS));
        } else {
            g2.setColor(new Color(35,35,35)); g2.fillOval(icX,icY,icS,icS);
            g2.setColor(rc.darker().darker()); g2.setStroke(new BasicStroke(Game.scale(2))); g2.drawOval(icX,icY,icS,icS);
            g2.setFont(bodyFont.deriveFont((float)Game.scale(16))); g2.setColor(new Color(100,100,100));
            drawC(g2,"?",new Rectangle(icX,icY,icS,icS));
        }

        int tx=icX+icS+Game.scale(6);
        g2.setFont(smallFont.deriveFont(Font.BOLD)); g2.setColor(u?Color.WHITE:new Color(110,110,110));
        drawClip(g2,type.displayName,tx,r.y+Game.scale(16),r.x+r.width-tx-Game.scale(4));
        int bw=Game.scale(60),bh=Game.scale(13);
        g2.setColor(new Color(rc.getRed(),rc.getGreen(),rc.getBlue(),50)); g2.fillRoundRect(tx,r.y+Game.scale(24),bw,bh,Game.scale(4),Game.scale(4));
        g2.setFont(tinyFont.deriveFont(Font.BOLD)); g2.setColor(rc);
        drawC(g2,type.rarity.name,new Rectangle(tx,r.y+Game.scale(24),bw,bh));
        if (u) {
            Pet pet=inventory.getPet(type);
            g2.setFont(tinyFont); g2.setColor(new Color(200,200,100));
            drawOutlined(g2, "Lv "+pet.getLevel()+"/"+Pet.MAX_LEVEL,tx,r.y+Game.scale(48));
            g2.setColor(new Color(140,185,255)); drawOutlined(g2, "x"+inventory.getTotalCopies(type),tx,r.y+Game.scale(60));
        } else {
            g2.setFont(tinyFont); g2.setColor(new Color(140,80,80)); drawOutlined(g2, "Locked",tx,r.y+Game.scale(48));
        }
        if (sel) { g2.setFont(tinyFont.deriveFont(Font.BOLD)); g2.setColor(new Color(80,220,80)); drawOutlined(g2, "*",r.x+r.width-Game.scale(14),r.y+Game.scale(14)); }
    }

    private void drawDetailStrip(Graphics2D g2, Pet.PetType[] types) {
        int dx=contentX(), dy=contentY()+contentH()-DETAIL_H;
        g2.setColor(new Color(8,6,16,230)); g2.fillRoundRect(dx,dy,contentW(),DETAIL_H,Game.scale(10),Game.scale(10));

        if (selectedForInfo<0||selectedForInfo>=types.length) {
            g2.setFont(smallFont); g2.setColor(new Color(110,110,110));
            drawC(g2,"Click a pet card to see details & upgrade",new Rectangle(dx,dy,contentW(),DETAIL_H));
            return;
        }
        Pet.PetType type=types[selectedForInfo];
        boolean u=inventory.isUnlocked(type); Color rc=type.rarity.color;
        g2.setColor(rc); g2.setStroke(new BasicStroke(Game.scale(2))); g2.drawRoundRect(dx,dy,contentW(),DETAIL_H,Game.scale(10),Game.scale(10));

        g2.setFont(bodyFont.deriveFont(Font.BOLD)); g2.setColor(rc);
        drawOutlined(g2, type.displayName+" - "+type.rarity.name, dx+Game.scale(10), dy+Game.scale(16));
        g2.setFont(tinyFont); g2.setColor(new Color(200,200,200));
        drawOutlined(g2, type.description, dx+Game.scale(10), dy+Game.scale(30));

        if (u) {
            Pet pet=inventory.getPet(type);
            g2.setColor(new Color(130,190,255)); drawOutlined(g2, statLine(pet), dx+Game.scale(10), dy+Game.scale(44));
            if (pet.canUpgrade()) {
                int dn=pet.getDupesRequired(), dh=inventory.getDuplicateCount(type), cn=pet.getUpgradeCoinCost();
                g2.setColor(dh>=dn?new Color(80,220,80):new Color(220,80,80));
                drawOutlined(g2, "Dupes: "+dh+"/"+dn, dx+Game.scale(10), dy+Game.scale(58));
                g2.setColor(player.getCoins()>=cn?new Color(80,220,80):new Color(220,80,80));
                drawOutlined(g2, "Coins: "+player.getCoins()+"/"+cn, dx+Game.scale(110), dy+Game.scale(58));
            } else { g2.setColor(new Color(255,210,0)); drawOutlined(g2, "MAX LEVEL", dx+Game.scale(10), dy+Game.scale(58)); }

            boolean cu=pet.canUpgrade()&&inventory.hasEnoughDupes(type)&&player.getCoins()>=pet.getUpgradeCoinCost();
            Rectangle ub=upgradeBounds();
            g2.setColor(cu?new Color(50,110,50):new Color(50,38,38)); g2.fillRoundRect(ub.x,ub.y,ub.width,ub.height,Game.scale(8),Game.scale(8));
            g2.setColor(cu?new Color(80,200,80):new Color(100,80,80)); g2.setStroke(new BasicStroke(Game.scale(1))); g2.drawRoundRect(ub.x,ub.y,ub.width,ub.height,Game.scale(8),Game.scale(8));
            g2.setFont(smallFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE);
            drawC(g2,pet.canUpgrade()?"Upgrade "+pet.getUpgradeCoinCost():"MAX",ub);

            Rectangle sb=selectBounds(); boolean isSel=(type==inventory.getSelectedPetType());
            g2.setColor(isSel?new Color(28,70,28):new Color(28,48,75)); g2.fillRoundRect(sb.x,sb.y,sb.width,sb.height,Game.scale(8),Game.scale(8));
            g2.setColor(isSel?new Color(80,200,80):new Color(80,140,200)); g2.setStroke(new BasicStroke(Game.scale(1))); g2.drawRoundRect(sb.x,sb.y,sb.width,sb.height,Game.scale(8),Game.scale(8));
            g2.setFont(smallFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE);
            drawC(g2,isSel?"* Selected":"Select for run",sb);
        } else {
            g2.setColor(new Color(160,130,100)); drawOutlined(g2, lockedPreview(type), dx+Game.scale(10), dy+Game.scale(44));
            g2.setColor(new Color(120,120,120)); drawOutlined(g2, "Get from eggs!", dx+Game.scale(10), dy+Game.scale(58));
        }
    }

    private void drawSpinAnim(Graphics2D g2) {
        int cx=getWidth()/2, cy=getHeight()/2;
        int sw=Game.scale(540),sh=Game.scale(90),cw=Game.scale(76),cg=Game.scale(8);
        g2.setColor(new Color(0,0,0,200)); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setFont(titleFont.deriveFont(Font.BOLD,(float)Game.scale(26))); g2.setColor(new Color(255,210,60));
        String lb="Opening egg..."; drawOutlined(g2, lb,cx-fw(g2,lb)/2,cy-sh/2-Game.scale(22));
        g2.setColor(new Color(14,10,24)); g2.fillRoundRect(cx-sw/2,cy-sh/2,sw,sh,Game.scale(14),Game.scale(14));
        g2.setColor(new Color(90,60,25)); g2.setStroke(new BasicStroke(Game.scale(3))); g2.drawRoundRect(cx-sw/2,cy-sh/2,sw,sh,Game.scale(14),Game.scale(14));
        Shape oc=g2.getClip(); g2.setClip(cx-sw/2+2,cy-sh/2+2,sw-4,sh-4);
        int vis=7,tcw=cw+cg,sx=cx-(vis/2)*tcw-tcw/2;
        for (int slot=0;slot<vis;slot++) {
            int pi=Math.max(0,Math.min(spinPool.size()-1,(spinIdx-vis/2+slot+spinPool.size()*10)%spinPool.size()));
            Pet.PetType pt=spinPool.get(pi);
            int cardX=sx+slot*tcw, cardY=cy-sh/2+Game.scale(7), ch2=sh-Game.scale(14);
            boolean ic=(slot==vis/2); Color rc=pt.rarity.color;
            g2.setColor(ic?rc.darker():new Color(30,22,42)); g2.fillRoundRect(cardX,cardY,cw,ch2,Game.scale(8),Game.scale(8));
            g2.setColor(ic?Color.WHITE:rc.darker()); g2.setStroke(new BasicStroke(ic?Game.scale(3):Game.scale(1))); g2.drawRoundRect(cardX,cardY,cw,ch2,Game.scale(8),Game.scale(8));
            g2.setColor(rc); int ics=Game.scale(30);
            g2.fillOval(cardX+(cw-ics)/2,cardY+Game.scale(8),ics,ics);
            g2.setColor(Color.WHITE); g2.setFont(bodyFont.deriveFont(Font.BOLD));
            drawC(g2,String.valueOf(pt.displayName.charAt(0)),new Rectangle(cardX+(cw-ics)/2,cardY+Game.scale(8),ics,ics));
            g2.setFont(tinyFont); g2.setColor(ic?Color.WHITE:new Color(150,150,150));
            drawClip(g2,pt.displayName,cardX+Game.scale(2),cardY+ch2-Game.scale(10),cw-Game.scale(4));
        }
        g2.setClip(oc);
        g2.setColor(new Color(255,210,60));
        g2.fillPolygon(new int[]{cx-Game.scale(8),cx+Game.scale(8),cx},new int[]{cy-sh/2-Game.scale(10),cy-sh/2-Game.scale(10),cy-sh/2},3);
    }

    private void drawResult(Graphics2D g2) {
        if (openResult==null) return;
        Pet.PetType type=openResult;
        boolean already=inventory.getTotalCopies(type)>1;
        int cx=getWidth()/2,cy=getHeight()/2,bw=Game.scale(320),bh=Game.scale(220),bx=cx-bw/2,by=cy-bh/2;
        g2.setColor(new Color(0,0,0,210)); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(new Color(18,12,28)); g2.fillRoundRect(bx,by,bw,bh,Game.scale(20),Game.scale(20));
        g2.setColor(type.rarity.color); g2.setStroke(new BasicStroke(Game.scale(4))); g2.drawRoundRect(bx,by,bw,bh,Game.scale(20),Game.scale(20));
        Color rc=type.rarity.color;
        g2.setColor(new Color(rc.getRed(),rc.getGreen(),rc.getBlue(),40)); g2.fillRoundRect(bx,by,bw,Game.scale(38),Game.scale(20),Game.scale(20));
        g2.setFont(bodyFont.deriveFont(Font.BOLD)); g2.setColor(rc);
        drawC(g2,type.rarity.name.toUpperCase(),new Rectangle(bx,by,bw,Game.scale(38)));
        int icS=Game.scale(60);
        g2.setColor(rc.darker()); g2.fillOval(cx-icS/2,by+Game.scale(44),icS,icS);
        g2.setColor(rc); g2.fillOval(cx-icS/2+Game.scale(3),by+Game.scale(47),icS-Game.scale(6),icS-Game.scale(6));
        g2.setFont(titleFont.deriveFont(Font.BOLD,(float)Game.scale(26))); g2.setColor(Color.WHITE);
        drawC(g2,String.valueOf(type.displayName.charAt(0)),new Rectangle(cx-icS/2,by+Game.scale(44),icS,icS));
        g2.setFont(titleFont.deriveFont(Font.BOLD)); g2.setColor(Color.WHITE);
        drawC(g2,type.displayName,new Rectangle(bx,by+Game.scale(112),bw,Game.scale(20)));
        g2.setFont(smallFont); g2.setColor(already?new Color(200,200,100):new Color(80,220,80));
        drawC(g2,already?"+"+inventory.getDuplicateCount(type)+" duplicate!":"New pet unlocked!",new Rectangle(bx,by+Game.scale(136),bw,Game.scale(16)));
        g2.setFont(tinyFont); g2.setColor(new Color(180,180,180));
        drawC(g2,type.description,new Rectangle(bx,by+Game.scale(154),bw,Game.scale(14)));
        g2.setColor(new Color(130,130,130));
        drawC(g2,"Click to continue",new Rectangle(bx,by+bh-Game.scale(22),bw,Game.scale(16)));
    }

    private void drawFooter(Graphics2D g2) {
        int px=panelX(),py=panelY(),fy=py+PANEL_H-FOOTER_H;
        g2.setColor(new Color(0,0,0,60)); g2.fillRect(px,fy,PANEL_W,FOOTER_H);
        g2.setColor(new Color(90,60,25,100)); g2.fillRect(px+Game.scale(6),fy,PANEL_W-Game.scale(12),Game.scale(2));
        g2.setFont(bodyFont.deriveFont(Font.BOLD)); g2.setColor(new Color(255,220,50));
        drawOutlined(g2, "Coins: "+player.getCoins(), px+CONTENT_X_OFF+Game.scale(6), fy+Game.scale(24));
        if (!statusMsg.isEmpty()&&System.currentTimeMillis()-statusTime<STATUS_MS) {
            long el=System.currentTimeMillis()-statusTime;
            float al=el<STATUS_MS-600?1f:1f-(el-(STATUS_MS-600))/600f;
            Composite oc=g2.getComposite(); g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, Math.max(0f, al))));
            g2.setFont(smallFont); g2.setColor(new Color(255,240,120));
            drawOutlined(g2, statusMsg,px+PANEL_W-fw(g2,statusMsg)-Game.scale(16),fy+Game.scale(24));
            g2.setComposite(oc);
        } else if (System.currentTimeMillis()-statusTime>=STATUS_MS) statusMsg="";
        Pet.PetType sel=inventory.getSelectedPetType();
        if (sel!=null) {
            g2.setFont(tinyFont.deriveFont(Font.BOLD)); g2.setColor(new Color(80,200,80));
            String ss="Next run: "+sel.displayName; drawOutlined(g2, ss,px+(PANEL_W-fw(g2,ss))/2,fy+Game.scale(24));
        }
    }

    private String statLine(Pet pet) {
        return switch (pet.getType().statBonus) {
            case HP_BONUS     -> "+HP: +"+pet.getCurrentStatValue()+" per level";
            case REGEN        -> "Regen: "+pet.getCurrentStatValue()+" HP/s";
            case SPEED        -> "+Speed: +"+pet.getCurrentStatValue();
            case DEFENSE      -> "+Defense: +"+pet.getCurrentStatValue();
            case DAMAGE_BONUS -> "+Proj dmg: +"+pet.getCurrentStatValue();
            case ATTACK_PET   -> "Attacks: "+pet.getCurrentAttackDmg()+" dmg / 1.6s";
            case ALL_STATS    -> "All +"+pet.getCurrentStatValue()+" & "+pet.getCurrentAttackDmg()+" atk";
        };
    }
    private String lockedPreview(Pet.PetType t) {
        return switch (t.statBonus) {
            case HP_BONUS     -> "+"+(int)(t.baseStatValue*t.rarity.statMult)+" HP at Lv1";
            case REGEN        -> (int)(t.baseStatValue*t.rarity.statMult)+" HP/s at Lv1";
            case SPEED        -> "+"+(int)(t.baseStatValue*t.rarity.statMult)+" speed at Lv1";
            case DEFENSE      -> "+"+(int)(t.baseStatValue*t.rarity.statMult)+" def at Lv1";
            case DAMAGE_BONUS -> "+"+(int)(t.baseStatValue*t.rarity.statMult)+" dmg at Lv1";
            case ATTACK_PET   -> (int)(t.baseAttackDmg*t.rarity.statMult)+" atk dmg at Lv1";
            case ALL_STATS    -> "All stats boosted";
        };
    }

    private void drawC(Graphics2D g2, String t, Rectangle r) {
        FontMetrics fm=g2.getFontMetrics();
        int tx = r.x+(r.width-fm.stringWidth(t))/2;
        int ty = r.y+(r.height+fm.getAscent()-fm.getDescent())/2;
        drawOutlined(g2, t, tx, ty);
    }
    private void drawClip(Graphics2D g2, String t, int x, int y, int mw) {
        FontMetrics fm=g2.getFontMetrics();
        while (fm.stringWidth(t)>mw&&t.length()>1) t=t.substring(0,t.length()-1);
        drawOutlined(g2, t, x, y);
    }
    private int fw(Graphics2D g2, String s) { return g2.getFontMetrics().stringWidth(s); }

    private void drawOutlined(Graphics2D g2, String text, int x, int y) {
        Color fg = g2.getColor();
        int o = Math.max(1, Game.scale(2));
        g2.setColor(new Color(0, 0, 0, 210));
        g2.drawString(text, x - o, y);
        g2.drawString(text, x + o, y);
        g2.drawString(text, x, y - o);
        g2.drawString(text, x, y + o);
        g2.drawString(text, x - o, y - o);
        g2.drawString(text, x + o, y - o);
        g2.drawString(text, x - o, y + o);
        g2.drawString(text, x + o, y + o);
        g2.setColor(fg);
        g2.drawString(text, x, y);
    }
}