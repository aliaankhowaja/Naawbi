JAVAC       = javac
JAVA        = java
LIB         = lib
BIN         = bin
SRC         = src
TEST        = test
TEST_BIN    = $(BIN)/test
MAIN_CLASS  = naawbi.Main
JUNIT_JAR   = $(LIB)/junit-platform-console-standalone-1.11.4.jar

# Classpath separator: ; on Windows, : on Unix (java.exe ignores : because of drive letters)
ifeq ($(OS),Windows_NT)
    CP_SEP := ;
else
    CP_SEP := :
endif

MODULE_PATH = --module-path $(LIB)
ADD_MODULES = --add-modules javafx.controls,javafx.fxml
CP          = $(LIB)/postgresql-42.7.4.jar
JAVA_LIB    = -Djava.library.path=$(LIB)

JAVAC_FLAGS = $(MODULE_PATH) $(ADD_MODULES) -cp "$(CP)" -d $(BIN)
JAVA_FLAGS  = $(MODULE_PATH) $(ADD_MODULES) -cp "$(BIN)$(CP_SEP)$(CP)" $(JAVA_LIB) --enable-native-access=javafx.graphics
DEV_FLAGS   = $(JAVA_FLAGS) -Dprism.order=sw -Dprism.verbose=true

.PHONY: all compile run dev clean clean-docs clean-test sources test test-sources seed help

all: compile

## Compile all Java sources and copy resources to bin/
compile: sources
	@echo "Compiling..."
	$(JAVAC) $(JAVAC_FLAGS) @$(BIN)/sources.txt
	@cp -r $(SRC)/naawbi/view $(BIN)/naawbi/
	@echo "Done."

## Regenerate bin/sources.txt from all .java files
sources:
	@mkdir -p $(BIN)
	@find $(SRC) -name "*.java" > $(BIN)/sources.txt

## Run the app (compile first if needed)
run: compile
	$(JAVA) $(JAVA_FLAGS) $(MAIN_CLASS)

## Run in dev mode (software renderer, verbose JavaFX logging)
dev: compile
	$(JAVA) $(DEV_FLAGS) $(MAIN_CLASS)

## Compile and run all unit tests under test/
test: compile test-sources
	@echo "Compiling tests..."
	@mkdir -p $(TEST_BIN)
	$(JAVAC) $(MODULE_PATH) $(ADD_MODULES) -cp "$(BIN)$(CP_SEP)$(CP)$(CP_SEP)$(JUNIT_JAR)" -d $(TEST_BIN) @$(TEST_BIN)/sources.txt
	@echo "Running tests..."
	$(JAVA) $(MODULE_PATH) $(ADD_MODULES) -jar $(JUNIT_JAR) \
	  --class-path "$(TEST_BIN)$(CP_SEP)$(BIN)$(CP_SEP)$(CP)" \
	  --scan-class-path \
	  --details=tree --disable-banner

## Regenerate test/sources.txt from all .java files under test/
test-sources:
	@mkdir -p $(TEST_BIN)
	@find $(TEST) -name "*.java" > $(TEST_BIN)/sources.txt

## Delete compiled test classes only
clean-test:
	@echo "Cleaning $(TEST_BIN)..."
	@rm -rf $(TEST_BIN)
	@echo "Cleaned."

## Delete compiled .class files and LaTeX build artifacts from docs/
clean:
	@echo "Cleaning $(BIN)..."
	@rm -rf $(BIN)/naawbi $(TEST_BIN)
	@echo "Cleaning docs LaTeX artifacts..."
	@find docs -name "*.aux" -o -name "*.bbl" -o -name "*.bcf" -o -name "*.blg" \
	  -o -name "*.fdb_latexmk" -o -name "*.fls" -o -name "*.log" -o -name "*.out" \
	  -o -name "*.run.xml" -o -name "*.synctex.gz" -o -name "*.toc" \
	  | xargs rm -f 2>/dev/null; true
	@echo "Cleaned."

## Delete LaTeX artifacts from docs/ only (without touching bin/)
clean-docs:
	@echo "Cleaning docs LaTeX artifacts..."
	@find docs -name "*.aux" -o -name "*.bbl" -o -name "*.bcf" -o -name "*.blg" \
	  -o -name "*.fdb_latexmk" -o -name "*.fls" -o -name "*.log" -o -name "*.out" \
	  -o -name "*.run.xml" -o -name "*.synctex.gz" -o -name "*.toc" \
	  | xargs rm -f 2>/dev/null; true
	@echo "Cleaned."

## Full rebuild from scratch
rebuild: clean compile

## Load test data into the DB (wipes existing data first)
seed:
	@bash data/db-seed.sh

## Show this help
help:
	@echo ""
	@echo "  make compile  - compile all sources"
	@echo "  make run      - compile + run"
	@echo "  make dev      - compile + run (software renderer, verbose)"
	@echo "  make test     - compile + run JUnit unit tests under test/"
	@echo "  make clean       - delete compiled classes + docs LaTeX artifacts"
	@echo "  make clean-docs  - delete docs LaTeX artifacts only"
	@echo "  make clean-test  - delete compiled test classes only"
	@echo "  make rebuild     - clean + compile"
	@echo "  make seed     - wipe DB and load test data from seed.sql"
	@echo ""
	@echo "  DB: jdbc:postgresql://localhost:5432/naawbi  user=postgres  pass=postgres"
	@echo "  Accounts (password: password123):"
	@echo "    ali@naawbi.edu   instructor"
	@echo "    ibbi@naawbi.edu  student"
	@echo "    sara@naawbi.edu  student"
	@echo ""
