JAVAC       = javac
JAVA        = java
LIB         = lib
BIN         = bin
SRC         = src
MAIN_CLASS  = naawbi.Main

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

.PHONY: all compile run dev clean clean-docs sources seed help

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

## Delete compiled .class files and LaTeX build artifacts from docs/
clean:
	@echo "Cleaning $(BIN)..."
	@rm -rf $(BIN)/naawbi
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
	@echo "  make clean       - delete compiled classes + docs LaTeX artifacts"
	@echo "  make clean-docs  - delete docs LaTeX artifacts only"
	@echo "  make rebuild     - clean + compile"
	@echo "  make seed     - wipe DB and load test data from seed.sql"
	@echo ""
	@echo "  DB: jdbc:postgresql://localhost:5432/naawbi  user=postgres  pass=postgres"
	@echo "  Accounts (password: password123):"
	@echo "    ali@naawbi.edu   instructor"
	@echo "    ibbi@naawbi.edu  student"
	@echo "    sara@naawbi.edu  student"
	@echo ""
