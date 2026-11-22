import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.*;
import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.*;
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Key;
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Value;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Creates a native, editable Astah project from the compiled Logic Lab classes. */
public final class CreateAstah {
    private ProjectAccessor project;
    private BasicModelEditor model;
    private final Map<String,IClass> types = new LinkedHashMap<>();
    private final Map<String,Class<?>> javaTypes = new LinkedHashMap<>();
    private final Map<String,IPackage> packages = new LinkedHashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    private final List<IDiagram> diagrams = new ArrayList<>();
    private record Relation(String from, String to, IElement model) {}
    private static final String M="logic.model.", A="logic.algorithm.", E="logic.expression.", U="logic.ui.";

    public static void main(String[] args) {
        try { new CreateAstah().run(Path.of(args[0]), Path.of(args[1])); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    private void run(Path jar, Path output) throws Exception {
        project = AstahAPI.getAstahAPI().getProjectAccessor();
        project.create(output.toAbsolutePath().toString());
        model = project.getModelEditorFactory().getBasicModelEditor();
        project.getTransactionManager().beginTransaction();
        try {
            project.getProject().setName("Logic Lab - Topic 9");
            project.getProject().setDefinition("Educational demo: Quine-McCluskey, 3/4 variables, SOP/POS. Native diagrams generated through the official Astah API. Models and members reflect the demo source; UI widget internals and compiler-generated record methods are omitted from detail views.");
            for (String name : List.of("logic", "logic.ui", "logic.model", "logic.algorithm", "logic.expression")) {
                int dot=name.lastIndexOf('.');
                packages.put(name, model.createPackage(dot<0?project.getProject():packages.get(name.substring(0,dot)), dot<0?name:name.substring(dot+1)));
            }
            readClasses(jar);
            createRelations();
            createUseCase();
            createGeneral();
            createDetails();
            project.getTransactionManager().endTransaction();
        } catch (Exception error) { project.getTransactionManager().abortTransaction(); throw error; }
        project.validateProject(); project.save();
        System.out.println("CREATED: " + output + " | " + types.size()+" Java types | "+diagrams.size()+" diagrams");
        for (IDiagram diagram:diagrams) System.out.println("DIAGRAM: "+diagram.getName()+" | "+diagram.getPresentations().length+" presentations");
        project.close();
        // Re-open through the same official API, not just a ZIP-header check.
        project.open(output.toAbsolutePath().toString());
        int actual=project.findElements(IDiagram.class).length;
        if(actual!=diagrams.size()) throw new IllegalStateException("Diagram count changed after reopen: "+actual);
        System.out.println("VERIFIED: reopened project, "+actual+" diagrams");
        project.close();
    }

    private void readClasses(Path jar) throws Exception {
        try (URLClassLoader loader=new URLClassLoader(new URL[]{jar.toUri().toURL()}); JarFile file=new JarFile(jar.toFile())) {
            List<String> names=file.stream().map(JarEntry::getName).filter(n->n.startsWith("logic/")&&n.endsWith(".class")&&!n.matches(".*\\$\\d.*"))
                    .map(n->n.substring(0,n.length()-6).replace('/','.')).sorted(Comparator.comparingInt((String n)->n.contains("$")?1:0).thenComparing(n->n)).toList();
            for(String name:names) {
                Class<?> source=loader.loadClass(name); if(source.isSynthetic()||source.isAnonymousClass()) continue;
                javaTypes.put(name,source);
                IClass owner=source.getEnclosingClass()==null?null:types.get(source.getEnclosingClass().getName());
                IClass target;
                if(owner==null) target=source.isInterface()?model.createInterface(packages.get(source.getPackageName()),source.getSimpleName()):model.createClass(packages.get(source.getPackageName()),source.getSimpleName());
                else target=source.isInterface()?model.createInterface(owner,source.getSimpleName()):model.createClass(owner,source.getSimpleName());
                target.setAbstract(Modifier.isAbstract(source.getModifiers())&&!source.isInterface());
                if(source.isRecord()) target.addStereotype("record");
                if(Modifier.isFinal(source.getModifiers())) target.setLeaf(true);
                target.setDefinition("Source: sourcecode/src/"+source.getName().split("\\$")[0].replace('.','/')+".java");
                types.put(name,target);
            }
            for(var entry:javaTypes.entrySet()) {
                Class<?> source=entry.getValue(); IClass target=types.get(entry.getKey());
                for(Field field:source.getDeclaredFields()) {
                    if(field.isSynthetic()||!includeField(source,field)) continue;
                    IAttribute attribute=model.createAttribute(target,field.getName(),shortType(field.getGenericType()));
                    attribute.setVisibility(visibility(field.getModifiers())); attribute.setStatic(Modifier.isStatic(field.getModifiers()));
                    attribute.setChangeable(!Modifier.isFinal(field.getModifiers()));
                }
                List<Method> methods=Arrays.stream(source.getDeclaredMethods()).filter(method->!method.isSynthetic()&&!method.isBridge()&&includeMethod(source,method))
                        .sorted(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount)).toList();
                for(Method method:methods) {
                    IOperation operation=model.createOperation(target,method.getName(),shortType(method.getGenericReturnType()));
                    operation.setVisibility(visibility(method.getModifiers())); operation.setStatic(Modifier.isStatic(method.getModifiers()));
                    operation.setAbstract(Modifier.isAbstract(method.getModifiers()));
                    Type[] params=method.getGenericParameterTypes();
                    for(int i=0;i<params.length;i++) model.createParameter(operation,parameterName(method.getName(),i,params.length),shortType(params[i]));
                }
            }
        }
    }
    private boolean includeField(Class<?> owner,Field field) {
        if(owner.getName().equals(U+"NormalizerFrame")) return List.of("controller","input","result","updating").contains(field.getName());
        return !Modifier.isStatic(field.getModifiers());
    }
    private boolean includeMethod(Class<?> owner,Method method) {
        String name=method.getName();
        if(owner.isRecord()&&(List.of("equals","hashCode","toString").contains(name)||Arrays.stream(owner.getRecordComponents()).anyMatch(c->c.getName().equals(name)))) return false;
        if(owner.getName().equals(U+"NormalizerFrame")) return List.of("calculate","replaceInput","markDirty","loadSample","showRound","showChart","showOverview").contains(name);
        return !Modifier.isPrivate(method.getModifiers());
    }
    private static String shortType(Type type) { return type.getTypeName().replace('$','.').replaceAll("(?:[a-z_][a-zA-Z0-9_]*\\.)+",""); }
    private static String visibility(int modifiers) { return Modifier.isPublic(modifiers)?"public":Modifier.isProtected(modifiers)?"protected":Modifier.isPrivate(modifiers)?"private":"package"; }
    private static String parameterName(String method,int index,int count) {
        if(method.equals("minimize")) return index==0?"input":"form";
        if(List.of("evaluate").contains(method)) return index==0?"row":"variableCount";
        if(List.of("combine").contains(method)) return "other";
        if(method.equals("covers")) return count==1?"row":index==0?"primeIndex":"row";
        if(List.of("output","toggle","showRound").contains(method)) return "row";
        if(List.of("targetRows","setTable","replaceInput").contains(method)) return "table";
        if(method.equals("build")) return "chosen";
        if(method.equals("expressionFor")) return "term";
        if(method.equals("solve")) return "chart";
        return "arg"+index;
    }
    private void rel(String from,String to,String kind) throws Exception {
        IClass source=types.get(from),target=types.get(to); IElement relation;
        if(kind.equals("extends")) relation=model.createGeneralization(source,target,"");
        else if(kind.equals("implements")) relation=model.createRealization(source,target,"");
        else if(kind.equals("uses")) {
            IDependency dependency=model.createDependency(target,source,"");
            if(!dependency.getClient().equals(source)) throw new IllegalStateException("Dependency direction: "+dependency.getClient().getName()+" -> "+dependency.getSupplier().getName());
            relation=dependency;
        }
        else {
            IAssociation association=model.createAssociation(source,target,"","","");
            for(IAttribute end:association.getMemberEnds()) {
                end.setMultiplicityString(end.getType().equals(target)?(kind.equals("many")?"0..*":from.equals(U+"NormalizerFrame")&&to.equals(M+"MinimizationResult")?"0..1":"1"):"0..*");
            }
            relation=association;
        }
        relations.add(new Relation(from,to,relation));
    }
    private void createRelations() throws Exception {
        rel("logic.Main",U+"NormalizerFrame","uses"); rel(U+"NormalizerFrame",U+"MainController","one");
        rel(U+"NormalizerFrame",U+"TruthTableModel","one"); rel(U+"NormalizerFrame",M+"MinimizationResult","one");
        rel(U+"TruthTableModel",M+"TruthTable","uses"); rel(U+"MainController",A+"QuineMcCluskeyEngine","one");
        rel(A+"QuineMcCluskeyEngine",A+"CoverSolver","one"); rel(A+"QuineMcCluskeyEngine",M+"TruthTable","uses");
        rel(A+"QuineMcCluskeyEngine",E+"NormalFormStrategy","uses"); rel(A+"QuineMcCluskeyEngine",M+"MinimizationResult","uses");
        rel(A+"CoverSolver",M+"PrimeImplicantChart","uses"); rel(A+"CoverSolver",A+"CoverSolver$Solution","uses");
        rel(M+"CombinationRound",M+"CombinationRound$Entry","many"); rel(M+"CombinationRound",M+"CombinationRound$Merge","many");
        rel(M+"CombinationRound$Entry",M+"Implicant","one"); rel(M+"CombinationRound$Merge",M+"Implicant","one");
        rel(M+"PrimeImplicantChart",M+"Implicant","many"); rel(M+"MinimizationResult",M+"TruthTable","one");
        rel(M+"MinimizationResult",M+"CombinationRound","many"); rel(M+"MinimizationResult",M+"PrimeImplicantChart","one");
        rel(M+"MinimizationResult",A+"CoverSolver$Solution","one"); rel(M+"MinimizationResult",E+"NormalFormStrategy","one");
        rel(M+"MinimizationResult",E+"BooleanExpression","one");
        rel(E+"SopStrategy",E+"NormalFormStrategy","extends"); rel(E+"PosStrategy",E+"NormalFormStrategy","extends");
        rel(E+"NormalFormStrategy",E+"BooleanExpression","uses");
        for(String part:List.of("Literal","Constant","And","Or")) rel(E+"BooleanExpression$"+part,E+"BooleanExpression","implements");
        rel(E+"BooleanExpression$And",E+"BooleanExpression","many"); rel(E+"BooleanExpression$Or",E+"BooleanExpression","many");
    }

    private void createUseCase() throws Exception {
        IPackage requirements=model.createPackage(project.getProject(),"requirements");
        UseCaseModelEditor editor=project.getModelEditorFactory().getUseCaseModelEditor();
        UseCaseDiagramEditor diagram=project.getDiagramEditorFactory().getUseCaseDiagramEditor();
        diagrams.add(diagram.createUseCaseDiagram(requirements,"01 UseCase"));
        INodePresentation boundary=diagram.createRect(new Point2D.Double(300,100),540,1070);
        boundary.setProperty(Key.FILL_COLOR,"#F4F9F8");
        INodePresentation boundaryTitle=diagram.createText("Logic Lab",new Point2D.Double(320,115));styleText(boundaryTitle,18,"#006D64");
        INodePresentation heading=diagram.createText("LOGIC LAB  /  USE CASES",new Point2D.Double(35,25)); styleText(heading,24,"#006D64");
        IClass actor=editor.createActor(requirements,"Người dùng");
        INodePresentation person=diagram.createNodePresentation(actor,new Point2D.Double(65,550));
        String[] names={"Chọn số biến và dạng SOP/POS","Nhập bảng chân trị 0/1","Rút gọn và xem kết quả","Xem các vòng ghép","Xem bảng PI và tập phủ","Nạp ví dụ / tạo ngẫu nhiên","Sao chép kết quả","Xem giải thích thuật ngữ"};
        for(int i=0;i<names.length;i++) {
            IUseCase useCase=editor.createUseCase(requirements,names[i]);
            useCase.setDefinition(switch(i) {
                case 0 -> "Chọn 3 hoặc 4 biến và dạng đầu ra SOP/POS.";
                case 1 -> "Nhập 8/16 giá trị F; chỉ 0/1, không cho phép don't-care.";
                case 2 -> "Chạy Quine-McCluskey, chọn phủ tối thiểu, kiểm chứng và hiển thị biểu thức F.";
                case 3 -> "Xem từng cột, nhóm số bit 1, các cặp ghép và prime implicant.";
                case 4 -> "Xem quan hệ phủ, essential PI và lựa chọn bổ sung bằng brute force.";
                default -> "Chức năng bổ sung của bản demo để hỗ trợ học và khám phá.";
            });
            INodePresentation node=diagram.createNodePresentation(useCase,new Point2D.Double(375,160+i*122));
            node.setWidth(390); node.setHeight(74);node.setProperty(Key.FILL_COLOR,"#FFFFFF");
            IAssociation association=model.createAssociation(actor,useCase,"","","");
            ILinkPresentation link=diagram.createLinkPresentation(association,person,node);
            double personX=person.getRectangle().getCenterX(),personY=person.getRectangle().getCenterY();
            double termY=node.getRectangle().getCenterY(),lane=240-i*12;
            link.setProperty(Key.LINE_SHAPE,Value.LINE_SHAPE_LINE_RIGHT_ANGLE);
            link.setAllPoints(new Point2D[]{new Point2D.Double(personX,personY),new Point2D.Double(lane,personY),new Point2D.Double(lane,termY),new Point2D.Double(node.getRectangle().getCenterX(),termY)});
        }
        INodePresentation note=diagram.createNote("Phạm vi bắt buộc: 3/4 biến; SOP/POS;\nbảng chân trị 0/1; các cột ghép; bảng PI;\nbiểu thức rút gọn. Không cần mạch logic.\n\nVí dụ, ngẫu nhiên, sao chép và trợ giúp\nlà chức năng bổ sung của bản demo.",new Point2D.Double(900,160));
        note.setWidth(380);
    }

    private void createGeneral() throws Exception {
        ClassDiagramEditor editor=project.getDiagramEditorFactory().getClassDiagramEditor();
        diagrams.add(editor.createClassDiagram(project.getProject(),"02 GeneralClass"));
        title(editor,"LOGIC LAB  /  GENERAL CLASS DIAGRAM","Tên lớp và quan hệ chính • không hiển thị thuộc tính / phương thức");
        Map<String,INodePresentation> nodes=new LinkedHashMap<>();
        String[][] groups={{"logic.Main",U+"NormalizerFrame",U+"MainController",U+"TruthTableModel"},
                {A+"QuineMcCluskeyEngine",A+"CoverSolver",A+"CoverSolver$Solution"},
                {M+"TruthTable",M+"Implicant",M+"CombinationRound",M+"CombinationRound$Entry",M+"CombinationRound$Merge",M+"PrimeImplicantChart",M+"MinimizationResult"},
                {E+"NormalFormStrategy",E+"SopStrategy",E+"PosStrategy",E+"BooleanExpression",E+"BooleanExpression$Literal",E+"BooleanExpression$Constant",E+"BooleanExpression$And",E+"BooleanExpression$Or"}};
        String[] labels={"UI + ENTRY POINT","ALGORITHM","MODEL","EXPRESSION"};
        for(int col=0;col<groups.length;col++) {
            INodePresentation heading=editor.createText(labels[col],new Point2D.Double(60+col*450,120));styleText(heading,18,"#006D64");
            for(int row=0;row<groups[col].length;row++) nodes.put(groups[col][row],node(editor,groups[col][row],60+col*450,200+row*150,330,false));
        }
        links(editor,nodes);
    }

    private void createDetails() throws Exception {
        detail("03 DetailModel",new String[][]{{M+"TruthTable",M+"PrimeImplicantChart"},{M+"Implicant",M+"MinimizationResult"},{M+"CombinationRound",M+"CombinationRound$Entry",M+"CombinationRound$Merge"}},"Dữ liệu bất biến, implicant, các vòng ghép và kết quả",true);
        detail("04 DetailAlgorithm",new String[][]{{A+"QuineMcCluskeyEngine",M+"TruthTable",M+"MinimizationResult"},{A+"CoverSolver",A+"CoverSolver$Solution",M+"PrimeImplicantChart"},{E+"NormalFormStrategy"}},"Ghép term → PI chart → essential PI → brute force → kiểm chứng",false);
        detail("05 DetailExpression",new String[][]{{E+"NormalFormStrategy",E+"SopStrategy",E+"PosStrategy"},{E+"BooleanExpression",E+"BooleanExpression$Literal",E+"BooleanExpression$Constant"},{E+"BooleanExpression$And",E+"BooleanExpression$Or"}},"Kế thừa, đa hình và cây biểu thức Boolean; POS dùng De Morgan",true);
        detail("06 DetailUI",new String[][]{{"logic.Main",U+"NormalizerFrame"},{U+"MainController",U+"TruthTableModel"},{M+"TruthTable",M+"MinimizationResult",A+"QuineMcCluskeyEngine"}},"Các lớp điều phối và table model; lược bỏ field Swing widget và helper bố cục",false);
    }
    private void detail(String name,String[][] columns,String subtitle,boolean allDetail) throws Exception {
        ClassDiagramEditor editor=project.getDiagramEditorFactory().getClassDiagramEditor();
        diagrams.add(editor.createClassDiagram(project.getProject(),name)); title(editor,"LOGIC LAB  /  "+name.substring(3).toUpperCase(Locale.ROOT),subtitle);
        Map<String,INodePresentation> nodes=new LinkedHashMap<>();
        for(int col=0;col<columns.length;col++) {
            double y=170;
            for(String key:columns[col]) {
                boolean full=allDetail||key.startsWith(name.contains("Algorithm")?A:U)||key.equals("logic.Main");
                INodePresentation node=node(editor,key,60+col*760,y,650,full); nodes.put(key,node);
                y+=Math.max(160,node.getHeight()+150);
            }
        }
        links(editor,nodes);
    }

    private INodePresentation node(ClassDiagramEditor editor,String key,double x,double y,double width,boolean detail) throws Exception {
        INodePresentation node=editor.createNodePresentation(types.get(key),new Point2D.Double(x,y));
        node.setProperty(Key.NOTATION_TYPE,Value.NOTATION_TYPE_NORMAL);
        node.setProperty(Key.FILL_COLOR,key.startsWith(M)?"#E8F3F0":key.startsWith(A)?"#EAF0FB":key.startsWith(E)?"#F0EBF8":"#FFF4DE");
        node.setProperty(Key.ATTRIBUTE_COMPARTMENT_VISIBILITY,Boolean.toString(detail));node.setProperty(Key.OPERATION_COMPARTMENT_VISIBILITY,Boolean.toString(detail));
        node.setProperty(Key.NAMESPACE_VISIBILITY,key.contains("$")?Value.NAMESPACE_VISIBILITY_UP_ONE_LEVEL:Value.NAMESPACE_VISIBILITY_NONE);
        node.setProperty(Key.AUTO_RESIZE,"false");node.setWidth(width);
        int lines=types.get(key).getAttributes().length+types.get(key).getOperations().length;
        node.setHeight(detail?Math.max(100,70+lines*24):key.contains("$")?85:70);
        node.setLocation(new Point2D.Double(x,y));
        return node;
    }
    private void links(ClassDiagramEditor editor,Map<String,INodePresentation> nodes) throws Exception {
        List<Rectangle2D> obstacles=nodes.values().stream().map(n->(Rectangle2D)new Rectangle2D.Double(n.getLocation().getX()-20,n.getLocation().getY()-20,n.getWidth()+40,n.getHeight()+40)).toList();
        int index=0;
        for(Relation relation:relations) {
            if(!nodes.containsKey(relation.from)||!nodes.containsKey(relation.to)) continue;
            INodePresentation source=nodes.get(relation.from),target=nodes.get(relation.to);
            // Astah expects parent/interface first for generalization/realization presentation.
            boolean reverse=relation.model instanceof IGeneralization||relation.model instanceof IRealization||relation.model instanceof IDependency;
            ILinkPresentation link=editor.createLinkPresentation(relation.model,reverse?target:source,reverse?source:target);
            link.setProperty(Key.LINE_COLOR,"#526A77");
            link.setProperty(Key.LINE_SHAPE,Value.LINE_SHAPE_LINE_RIGHT_ANGLE);
            Point2D[] bends=route(source,target,obstacles,index++);
            Point2D[] path=new Point2D[bends.length+2];
            path[0]=new Point2D.Double(source.getRectangle().getCenterX(),source.getRectangle().getCenterY());
            System.arraycopy(bends,0,path,1,bends.length);
            path[path.length-1]=new Point2D.Double(target.getRectangle().getCenterX(),target.getRectangle().getCenterY());
            if(reverse) { for(int l=0,r=path.length-1;l<r;l++,r--) {Point2D temp=path[l];path[l]=path[r];path[r]=temp;} }
            link.setAllPoints(path);
        }
    }
    /** Orthogonal A* routing around class rectangles, keeping lines out of text. */
    private Point2D[] route(INodePresentation a,INodePresentation b,List<Rectangle2D> obstacles,int index) {
        Rectangle2D ar=a.getRectangle(),br=b.getRectangle();
        double ax=ar.getCenterX(),ay=ar.getCenterY(),bx=br.getCenterX(),by=br.getCenterY();
        int sx,sy,tx,ty;
        if(Math.abs(ax-bx)<5) {
            sx=(int)Math.round(ax/20);tx=(int)Math.round(bx/20);
            sy=(int)(by>ay?Math.ceil((ar.getMaxY()+40)/20):Math.floor((ar.getMinY()-40)/20));
            ty=(int)(by>ay?Math.floor((br.getMinY()-40)/20):Math.ceil((br.getMaxY()+40)/20));
        } else {
            sy=(int)Math.round(ay/20);ty=(int)Math.round(by/20);
            sx=(int)(bx>ax?Math.ceil((ar.getMaxX()+40)/20):Math.floor((ar.getMinX()-40)/20));
            tx=(int)(bx>ax?Math.floor((br.getMinX()-40)/20):Math.ceil((br.getMaxX()+40)/20));
        }
        int width=(int)Math.ceil(obstacles.stream().mapToDouble(Rectangle2D::getMaxX).max().orElse(2000)/20)+12;
        int height=(int)Math.ceil(obstacles.stream().mapToDouble(Rectangle2D::getMaxY).max().orElse(2000)/20)+12;
        int start=sy*width+sx,goal=ty*width+tx;
        int[] distance=new int[width*height],previous=new int[width*height];Arrays.fill(distance,Integer.MAX_VALUE);Arrays.fill(previous,-1);
        record Step(int cell,int score){}
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt(Step::score).thenComparingInt(Step::cell));
        distance[start]=0;queue.add(new Step(start,0));
        int[][] moves={{1,0},{0,1},{-1,0},{0,-1}};
        while(!queue.isEmpty()) {
            int cell=queue.remove().cell;if(cell==goal)break;
            int x=cell%width,y=cell/width;
            for(int[] move:moves) {
                int nx=x+move[0],ny=y+move[1];if(nx<1||ny<7||nx>=width||ny>=height)continue;
                int next=ny*width+nx;
                boolean blocked=obstacles.stream().anyMatch(rect->rect.contains(nx*20.0,ny*20.0));
                if(blocked&&next!=goal)continue;
                int cost=distance[cell]+10;
                if(previous[cell]>=0) {
                    int px=previous[cell]%width,py=previous[cell]/width;
                    if(nx-x!=x-px||ny-y!=y-py)cost+=3;
                }
                if(cost>=distance[next])continue;
                distance[next]=cost;previous[next]=cell;
                queue.add(new Step(next,cost+10*(Math.abs(nx-tx)+Math.abs(ny-ty))));
            }
        }
        if(previous[goal]<0&&start!=goal)throw new IllegalStateException("No clear UML route: "+a.getLabel()+" -> "+b.getLabel());
        List<Point2D> points=new ArrayList<>();
        for(int cell=goal;cell>=0;cell=previous[cell]) {points.add(new Point2D.Double((cell%width)*20.0,(cell/width)*20.0));if(cell==start)break;}
        Collections.reverse(points);
        List<Point2D> compressed=new ArrayList<>();
        for(int i=0;i<points.size();i++) {
            if(i>0&&i+1<points.size()) {
                Point2D p=points.get(i-1),q=points.get(i),r=points.get(i+1);
                if((p.getX()==q.getX()&&q.getX()==r.getX())||(p.getY()==q.getY()&&q.getY()==r.getY()))continue;
            }
            compressed.add(points.get(i));
        }
        return compressed.toArray(Point2D[]::new);
    }
    private void title(ClassDiagramEditor editor,String title,String subtitle) throws Exception {
        INodePresentation top=editor.createText(title,new Point2D.Double(60,25)); styleText(top,26,"#006D64");
        INodePresentation sub=editor.createText(subtitle,new Point2D.Double(60,78));styleText(sub,16,"#536570");
    }
    private void styleText(INodePresentation text,int size,String color) throws Exception {
        text.setProperty(Key.FONT_COLOR,color);
    }
}
